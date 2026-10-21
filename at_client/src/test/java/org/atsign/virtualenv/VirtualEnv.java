package org.atsign.virtualenv;

import static java.util.concurrent.TimeUnit.HOURS;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.SECONDS;

import java.io.File;
import java.time.Duration;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.atsign.cucumber.helpers.Helpers;
import org.slf4j.Logger;
import org.testcontainers.containers.ComposeContainer;
import org.testcontainers.containers.Container.ExecResult;
import org.testcontainers.containers.ContainerState;
import org.testcontainers.containers.output.OutputFrame;
import org.testcontainers.containers.output.Slf4jLogConsumer;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class VirtualEnv {

  private static final String SERVICE = "virtualenv";

  private static final String ROOT_SERVER = "vip.ve.atsign.zone:64";

  private static final Duration PROVISIONING_LIMIT = Duration.ofSeconds(180);

  private static final Pattern HOSTED = Pattern.compile("/secondary\\s+-a\\s+(@\\S+)");

  private static final Lifecycle LIFECYCLE = new Lifecycle(VirtualEnv::startCompose,
      () -> Helpers.isHostPortReachable(ROOT_SERVER, SECONDS.toMillis(2)), PROVISIONING_LIMIT);

  public static void main(String[] args) throws Exception {
    setUp();
    log.info("sleeping for 1 hour, after which container will be torn down...");
    Thread.sleep(HOURS.toMillis(1));
    log.info("tearing down");
    tearDown();
  }

  /**
   * Starts the virtualenv, unless one is already answering on its root server, returning once
   * install_PKAM_Keys has exited having provisioned every atSign the container hosts. A failure
   * stops the container, and every later call rethrows it without starting another.
   */
  public static void setUp() {
    LIFECYCLE.setUp();
  }

  /**
   * Stops the virtualenv this class started, if any.
   */
  public static void tearDown() {
    LIFECYCLE.tearDown();
  }

  /**
   * Returns the consumer that writes each line of the container's output to the given logger,
   * prefixed {@code [virtualenv]}.
   */
  static Consumer<OutputFrame> containerLog(Logger logger) {
    return new Slf4jLogConsumer(logger).withPrefix(SERVICE);
  }

  /**
   * Returns the atSigns the container runs an atServer for, folded, as named in its supervisor
   * configuration.
   */
  static Set<String> hostedAtSigns(String supervisorConfig) {
    Matcher matcher = HOSTED.matcher(supervisorConfig);
    Set<String> hosted = new TreeSet<>();
    while (matcher.find()) {
      hosted.add(fold(matcher.group(1)));
    }
    return hosted;
  }

  private static String fold(String atSign) {
    return atSign.toLowerCase(Locale.ROOT);
  }

  private static Started startCompose(Consumer<OutputFrame> provisioningWatch) {
    ComposeContainer container;
    try {
      container = new ComposeContainer(new File(VirtualEnv.class.getResource("docker-compose.yml").toURI()))
          .withLogConsumer(SERVICE, containerLog(log))
          .withLogConsumer(SERVICE, provisioningWatch);
    } catch (Exception e) {
      throw new IllegalStateException("could not load the virtualenv compose file", e);
    }
    try {
      container.start();
    } catch (RuntimeException e) {
      try {
        container.stop();
      } catch (RuntimeException stopFailure) {
        e.addSuppressed(stopFailure);
      }
      throw e;
    }
    return new Started() {
      @Override
      public String supervisorConfig() throws Exception {
        ContainerState state = container.getContainerByServiceName(SERVICE)
            .orElseThrow(() -> new IllegalStateException("no " + SERVICE + " service in the compose project"));
        ExecResult result = state.execInContainer("sh", "-c", "cat /etc/supervisor/conf.d/*.conf");
        if (result.getExitCode() != 0) {
          throw new IllegalStateException("reading the supervisor configuration exited " + result.getExitCode());
        }
        return result.getStdout();
      }

      @Override
      public void stop() {
        container.stop();
      }
    };
  }

  /**
   * A started virtualenv container, as {@link Lifecycle} needs it.
   */
  interface Started {

    /**
     * Returns the text of the container's supervisor configuration.
     */
    String supervisorConfig() throws Exception;

    /**
     * Stops and removes the container.
     */
    void stop();
  }

  /**
   * Starts a virtualenv, at most once until it is torn down, and waits for it to be provisioned.
   */
  static final class Lifecycle {

    private final Function<Consumer<OutputFrame>, Started> starter;
    private final BooleanSupplier alreadyRunning;
    private final Duration limit;
    private Started started;
    private RuntimeException failure;

    Lifecycle(Function<Consumer<OutputFrame>, Started> starter, BooleanSupplier alreadyRunning, Duration limit) {
      this.starter = starter;
      this.alreadyRunning = alreadyRunning;
      this.limit = limit;
    }

    /**
     * Starts the virtualenv unless this lifecycle already has or one is already running, returning
     * once it is provisioned; throws, and stops the container, when it is not.
     */
    synchronized void setUp() {
      if (failure != null) {
        throw new IllegalStateException("the virtualenv failed to start earlier in this run: "
            + failure.getMessage(), failure);
      }
      if (started != null) {
        return;
      }
      if (alreadyRunning.getAsBoolean()) {
        log.info("using the virtualenv that is already running");
        return;
      }
      ProvisioningWatch watch = new ProvisioningWatch();
      Started candidate = null;
      try {
        candidate = starter.apply(watch);
        watch.awaitProvisioned(readHostedAtSigns(candidate), limit);
        started = candidate;
      } catch (Exception e) {
        if (e instanceof InterruptedException) {
          Thread.currentThread().interrupt();
        }
        failure = e instanceof RuntimeException ? (RuntimeException) e : new IllegalStateException(e);
        if (candidate != null) {
          try {
            candidate.stop();
          } catch (RuntimeException stopFailure) {
            failure.addSuppressed(stopFailure);
          }
        }
        throw failure;
      }
    }

    synchronized void tearDown() {
      if (started != null) {
        started.stop();
        started = null;
      }
    }

    private static Set<String> readHostedAtSigns(Started candidate) {
      try {
        return hostedAtSigns(candidate.supervisorConfig());
      } catch (Exception e) {
        throw new IllegalStateException("found no hosted atSigns: the virtualenv's supervisor configuration"
            + " could not be read", e);
      }
    }
  }

  /**
   * Records, from the container's output, each atSign install_PKAM_Keys reports provisioned, the
   * status it exits with, and whether the output has ended.
   */
  static final class ProvisioningWatch implements Consumer<OutputFrame> {

    private static final Pattern PROVISIONED =
        Pattern.compile("install_PKAM_Keys \\|(?:cramAndPkamAuth|cramAuthOnly) successful for (@\\S+)$");
    private static final Pattern EXITED = Pattern.compile("install_PKAM_Keys exited with status (\\d+)");

    private final Set<String> provisioned = ConcurrentHashMap.newKeySet();
    private final CountDownLatch exitedOrEnded = new CountDownLatch(1);
    private volatile boolean exitReported;
    private volatile int exitStatus;

    @Override
    public void accept(OutputFrame frame) {
      if (frame.getType() == OutputFrame.OutputType.END) {
        exitedOrEnded.countDown();
        return;
      }
      String line = frame.getUtf8StringWithoutLineEnding();
      Matcher matcher = PROVISIONED.matcher(line);
      if (matcher.find()) {
        provisioned.add(fold(matcher.group(1)));
        return;
      }
      matcher = EXITED.matcher(line);
      if (matcher.find()) {
        exitStatus = Integer.parseInt(matcher.group(1));
        exitReported = true;
        exitedOrEnded.countDown();
      }
    }

    /**
     * Returns once install_PKAM_Keys has exited with status 0 having provisioned every hosted
     * atSign; throws naming the hosted atSigns it has not provisioned otherwise, when the
     * container's output ends first, or once the limit passes.
     */
    void awaitProvisioned(Set<String> hosted, Duration limit) throws InterruptedException {
      if (hosted.isEmpty()) {
        throw new IllegalStateException("found no hosted atSigns in the virtualenv's supervisor configuration");
      }
      log.info("awaiting install_PKAM_Keys provisioning {} hosted atSigns...", hosted.size());
      boolean finished = exitedOrEnded.await(limit.toMillis(), MILLISECONDS);
      List<String> missing = hosted.stream()
          .filter(atSign -> !provisioned.contains(atSign))
          .collect(Collectors.toList());
      if (!finished) {
        throw new IllegalStateException(String.format(Locale.ROOT,
                                                      "install_PKAM_Keys had not exited after %.1fs; %d of %d hosted atSigns not yet provisioned: %s",
                                                      limit.toMillis() / 1000.0, missing.size(), hosted.size(),
                                                      missing));
      }
      if (!exitReported) {
        throw new IllegalStateException(String.format(Locale.ROOT,
                                                      "the virtualenv container's output ended before install_PKAM_Keys exited; %d of %d hosted atSigns"
                                                          + " not yet provisioned: %s",
                                                      missing.size(), hosted.size(), missing));
      }
      if (exitStatus != 0 || !missing.isEmpty()) {
        throw new IllegalStateException(String.format(Locale.ROOT,
                                                      "install_PKAM_Keys exited with status %d; %d of %d hosted atSigns not provisioned: %s",
                                                      exitStatus, missing.size(), hosted.size(), missing));
      }
      log.info("install_PKAM_Keys provisioned all {} hosted atSigns", hosted.size());
    }
  }
}
