package org.atsign.virtualenv;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockingDetails;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.helpers.MessageFormatter;
import org.testcontainers.containers.output.OutputFrame;
import org.testcontainers.containers.output.OutputFrame.OutputType;

class VirtualEnvTest {

  private static final String CONFIG = "[program:25026_colin]\n"
      + "command = /atsign/secondary/secondary -a @colin -p 25026 -s colinsecret\n"
      + "[program:25029_gary]\n"
      + "command = /atsign/secondary/secondary -a @gary -p 25029 -s garysecret\n"
      + "[program:pkamLoad]\n"
      + "command = /bin/sh -c /tmp/pkam.sh\n";

  private static final Duration LIMIT = Duration.ofSeconds(5);

  @Test
  void testSetUpReturnsOnlyAfterInstallPkamKeysExitsHavingProvisionedEveryHostedAtSign() {
    AtomicBoolean exitReported = new AtomicBoolean();
    FakeStarter starter = new FakeStarter(CONFIG, watch -> {
      watch.accept(stderr("SHOUT|2026-10-19 06:10:26.1| install_PKAM_Keys |cramAndPkamAuth successful for @colin"));
      watch.accept(stderr("SHOUT|2026-10-19 06:10:26.2| install_PKAM_Keys |cramAndPkamAuth successful for @gary"));
      new Thread(() -> {
        sleep(200);
        exitReported.set(true);
        watch.accept(stderr("install_PKAM_Keys exited with status 0"));
      }).start();
    });

    new VirtualEnv.Lifecycle(starter, LIMIT).setUp();

    assertThat("setUp returned before install_PKAM_Keys exited", exitReported.get(), is(true));
    assertThat(starter.stops.get(), equalTo(0));
  }

  @Test
  void testSetUpThrowsNamingTheHostedAtSignInstallPkamKeysExitedWithoutProvisioning() {
    FakeStarter starter = new FakeStarter(CONFIG, watch -> {
      watch.accept(stderr("SHOUT|2026-10-19 06:10:26.1| install_PKAM_Keys |cramAndPkamAuth successful for @colin"));
      watch.accept(stderr("install_PKAM_Keys exited with status 0"));
    });

    IllegalStateException ex = assertThrows(IllegalStateException.class,
                                            () -> new VirtualEnv.Lifecycle(starter, LIMIT).setUp());

    assertThat(ex.getMessage(), containsString("1 of 2 hosted atSigns not provisioned: [@gary]"));
  }

  @Test
  void testSetUpThrowsOnceTheLimitPassesNamingTheHostedAtSignsNotYetProvisioned() {
    FakeStarter starter = new FakeStarter(CONFIG, watch -> watch
        .accept(stderr("SHOUT|2026-10-19 06:10:26.1| install_PKAM_Keys |cramAuthOnly successful for @colin")));

    IllegalStateException ex = assertThrows(IllegalStateException.class,
                                            () -> new VirtualEnv.Lifecycle(starter, Duration.ofMillis(100))
                                                .setUp());

    assertThat(ex.getMessage(), containsString("had not exited after 0.1s"));
    assertThat(ex.getMessage(), containsString("1 of 2 hosted atSigns not yet provisioned: [@gary]"));
  }

  @Test
  void testContainerOutputIsLoggedPrefixedVirtualenv() {
    Logger logger = mock(Logger.class);

    VirtualEnv.containerLog(logger).accept(stderr("install_PKAM_Keys exited with status 0"));

    List<String> logged = mockingDetails(logger).getInvocations().stream()
        .filter(invocation -> invocation.getMethod().getName().equals("info"))
        .map(invocation -> render(invocation.getArguments()))
        .collect(Collectors.toList());
    assertThat(logged, contains("[virtualenv] STDERR: install_PKAM_Keys exited with status 0"));
  }

  @Test
  void testSetUpRethrowsTheFirstFailureWithoutStartingAnotherContainer() {
    FakeStarter starter =
        new FakeStarter(CONFIG, watch -> watch.accept(stderr("install_PKAM_Keys exited with status 0")));
    VirtualEnv.Lifecycle lifecycle = new VirtualEnv.Lifecycle(starter, LIMIT);
    IllegalStateException first = assertThrows(IllegalStateException.class, lifecycle::setUp);

    IllegalStateException later = assertThrows(IllegalStateException.class, lifecycle::setUp);

    assertThat(later.getMessage(), containsString(first.getMessage()));
    assertThat(starter.starts.get(), equalTo(1));
    assertThat(starter.stops.get(), equalTo(1));
  }

  @Test
  void testSetUpThrowsWhenTheSupervisorConfigurationNamesNoHostedAtSigns() {
    FakeStarter starter = new FakeStarter("[program:pkamLoad]\ncommand = /bin/sh -c /tmp/pkam.sh\n",
        watch -> watch.accept(stderr("install_PKAM_Keys exited with status 0")));

    IllegalStateException ex = assertThrows(IllegalStateException.class,
                                            () -> new VirtualEnv.Lifecycle(starter, LIMIT).setUp());

    assertThat(ex.getMessage(), containsString("found no hosted atSigns"));
  }

  @Test
  void testSetUpThrowsWhenTheSupervisorConfigurationCannotBeRead() {
    FakeStarter starter =
        new FakeStarter(null, watch -> watch.accept(stderr("install_PKAM_Keys exited with status 0")));

    IllegalStateException ex = assertThrows(IllegalStateException.class,
                                            () -> new VirtualEnv.Lifecycle(starter, LIMIT).setUp());

    assertThat(ex.getMessage(), containsString("found no hosted atSigns"));
  }

  private static OutputFrame stderr(String line) {
    return new OutputFrame(OutputType.STDERR, (line + "\n").getBytes(StandardCharsets.UTF_8));
  }

  private static String render(Object[] arguments) {
    return MessageFormatter.arrayFormat((String) arguments[0], Arrays.copyOfRange(arguments, 1, arguments.length))
        .getMessage();
  }

  private static void sleep(long millis) {
    try {
      Thread.sleep(millis);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }
  }

  private static final class FakeStarter implements Function<Consumer<OutputFrame>, VirtualEnv.Started> {

    private final String supervisorConfig;
    private final Consumer<Consumer<OutputFrame>> output;
    private final AtomicInteger starts = new AtomicInteger();
    private final AtomicInteger stops = new AtomicInteger();

    FakeStarter(String supervisorConfig, Consumer<Consumer<OutputFrame>> output) {
      this.supervisorConfig = supervisorConfig;
      this.output = output;
    }

    @Override
    public VirtualEnv.Started apply(Consumer<OutputFrame> watch) {
      starts.incrementAndGet();
      output.accept(watch);
      return new VirtualEnv.Started() {
        @Override
        public String supervisorConfig() throws Exception {
          if (supervisorConfig == null) {
            throw new IOException("cat: /etc/supervisor/conf.d/*.conf: No such file or directory");
          }
          return supervisorConfig;
        }

        @Override
        public void stop() {
          stops.incrementAndGet();
        }
      };
    }
  }
}
