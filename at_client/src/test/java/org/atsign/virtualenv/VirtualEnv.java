package org.atsign.virtualenv;

import static java.util.concurrent.TimeUnit.HOURS;
import static java.util.concurrent.TimeUnit.SECONDS;

import java.io.File;
import java.net.URL;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.ComposeContainer;
import org.testcontainers.containers.output.OutputFrame;

public class VirtualEnv {

  private static final Logger LOGGER = LoggerFactory.getLogger(VirtualEnv.class);

  private static ComposeContainer CONTAINER;

  public static void main(String[] args) throws Exception {
    setUp();
    LOGGER.info("sleeping for 1 hour, after which container will be torn down...");
    Thread.sleep(HOURS.toMillis(1));
    LOGGER.info("tearing down");
    tearDown();
  }

  public static void setUp() {
    try {
      URL resource = VirtualEnv.class.getResource("docker-compose.yml");
      StartUpLatch latch = new StartUpLatch(Pattern.compile("install_PKAM_Keys .*successful"), 40);
      CONTAINER = new ComposeContainer(new File(resource.toURI())).withLogConsumer("virtualenv", latch);
      CONTAINER.start();
      latch.await(20, SECONDS);
    } catch (Exception e) {
      CONTAINER = null;
      throw new RuntimeException(e);
    }
  }

  public static void tearDown() {
    CONTAINER.stop();
  }

  private static class StartUpLatch implements Consumer<OutputFrame> {

    private final Matcher matcher;
    private final CountDownLatch latch;

    public StartUpLatch(Pattern logPattern, int expectedMatches) {
      this.matcher = logPattern.matcher("");
      this.latch = new CountDownLatch(expectedMatches);
    };

    public void await(long timeout, TimeUnit unit) throws InterruptedException {
      LOGGER.info("awaiting container log to contain {} lines that match {}...", latch.getCount(), matcher.pattern());
      latch.await(timeout, unit);
      LOGGER.info("container log indicates that start up is complete");
    }

    @Override
    public void accept(OutputFrame outputFrame) {
      if (matcher.reset(outputFrame.getUtf8StringWithoutLineEnding()).find()) {
        latch.countDown();
      }
    }
  }
}
