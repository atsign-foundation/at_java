package org.atsign.client.cli;

import org.atsign.client.util.KeysUtil;
import org.atsign.cucumber.helpers.AtDemoData;
import org.atsign.cucumber.helpers.Helpers;
import org.atsign.virtualenv.VirtualEnv;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

class ActivateTest {

  private static final Logger LOGGER = LoggerFactory.getLogger(ActivateTest.class);

  private final ByteArrayOutputStream outBuffer = new ByteArrayOutputStream();
  private final ByteArrayOutputStream errBuffer = new ByteArrayOutputStream();
  private PrintStream originalStdout;
  private PrintStream originalStdErr;
  private ExecutorService executor;
  private int exitCode;

  @BeforeAll
  public static void classSetup() {
    if (!Helpers.isHostPortReachable("vip.ve.atsign.zone:64", SECONDS.toMillis(2))) {
      VirtualEnv.setUp();
    }
  }


  @BeforeEach
  public void setup() {
    executor = Executors.newSingleThreadExecutor();
    originalStdout = System.out;
    System.setOut(new PrintStream(outBuffer, true));
    originalStdErr = System.err;
    System.setErr(new PrintStream(errBuffer, true));
    System.setProperty("test.mode", "true");
  }

  @AfterEach
  public void teardown() throws IOException {
    System.setOut(originalStdout);
    System.setErr(originalStdErr);
    System.out.print(outBuffer);
    System.err.print(errBuffer);
    System.setProperty("test.mode", "false");
    executor.shutdown();
  }

  @Test
  public void testInvalidArgsReturnsNonZeroExitCodeAndUsageMessage() {
    assertExecuteNotSuccess(
                            "xyz",
                            "-a", "@srie",
                            "-r", "vip.ve.atsign.zone:64");
    assertBufferAndReset(errBuffer, containsString("expected one of [onboard, enroll, otp, list, approve"));
  }

  @Test
  public void testHelpReturnsZeroExitCodeAndUsageMessage() {
    assertExecuteSuccess("-h");
    assertBufferAndReset(outBuffer, startsWith("Usage: "));
  }

  @Test
  public void testOnboardEnrollApprove() throws IOException {
    File keys = new File(Files.createTempDirectory("test").toFile(), "test.atKeys");
    keys.deleteOnExit();
    assertExecuteSuccess(
                         "onboard",
                         "-a", "@srie",
                         "-r", "vip.ve.atsign.zone:64",
                         "-k", keys.getAbsolutePath(),
                         "-c", AtDemoData.getClassConst("at_demo_apkam_keys.dart", "SrieKeys", "_cramKey"));

    assertExecuteSuccess(
                         "otp",
                         "-a", "@srie",
                         "-r", "vip.ve.atsign.zone:64",
                         "-k", keys.getAbsolutePath());
    String otp = assertBufferFind(outBuffer, "^(\\S+)$");

    File appKeys = new File(Files.createTempDirectory("test").toFile(), "app.atKeys");
    appKeys.deleteOnExit();

    executor
        .submit(() -> testEnrollListAndRespond("@srie", keys, "approve", () -> getEnrollmentIdWhenAvailable(appKeys)));

    assertExecuteSuccess(
                         "enroll",
                         "-a", "@srie",
                         "-r", "vip.ve.atsign.zone:64",
                         "-p", "app",
                         "-d", "device-" + System.currentTimeMillis(),
                         "-n", "ns:rw",
                         "-k", appKeys.getAbsolutePath(),
                         "-s", otp);
  }

  @Test
  public void testOnboardEnrollDeny() throws IOException {
    File keys = new File(Files.createTempDirectory("test").toFile(), "test.atKeys");
    keys.deleteOnExit();
    assertExecuteSuccess(
                         "onboard",
                         "-a", "@srie",
                         "-r", "vip.ve.atsign.zone:64",
                         "-k", keys.getAbsolutePath(),
                         "-c", AtDemoData.getClassConst("at_demo_apkam_keys.dart", "SrieKeys", "_cramKey"));

    assertExecuteSuccess(
                         "otp",
                         "-a", "@srie",
                         "-r", "vip.ve.atsign.zone:64",
                         "-k", keys.getAbsolutePath());
    String otp = assertBufferFind(outBuffer, "^(\\S+)$");

    File appKeys = new File(Files.createTempDirectory("test").toFile(), "app.atKeys");
    appKeys.deleteOnExit();

    executor.submit(() -> testEnrollListAndRespond("@srie", keys, "deny", () -> getEnrollmentIdWhenAvailable(appKeys)));

    assertExecuteNotSuccess(
                            "enroll",
                            "-a", "@srie",
                            "-r", "vip.ve.atsign.zone:64",
                            "-p", "app",
                            "-d", "device-" + System.currentTimeMillis(),
                            "-n", "ns:rw",
                            "-k", appKeys.getAbsolutePath(),
                            "-s", otp);
    assertBufferAndReset(errBuffer, containsString("is denied"));
  }

  @Test
  public void testOnboardEnrollRevoke() throws IOException {
    File keys = new File(Files.createTempDirectory("test").toFile(), "test.atKeys");
    keys.deleteOnExit();
    assertExecuteSuccess(
                         "onboard",
                         "-a", "@srie",
                         "-r", "vip.ve.atsign.zone:64",
                         "-k", keys.getAbsolutePath(),
                         "-c", AtDemoData.getClassConst("at_demo_apkam_keys.dart", "SrieKeys", "_cramKey"));

    assertExecuteSuccess(
                         "otp",
                         "-a", "@srie",
                         "-r", "vip.ve.atsign.zone:64",
                         "-k", keys.getAbsolutePath());
    String otp = assertBufferFind(outBuffer, "^(\\S+)$");

    File appKeys = new File(Files.createTempDirectory("test").toFile(), "app.atKeys");
    appKeys.deleteOnExit();

    executor
        .submit(() -> testEnrollListAndRespond("@srie", keys, "approve", () -> getEnrollmentIdWhenAvailable(appKeys)));

    assertExecuteSuccess(
                         "enroll",
                         "-a", "@srie",
                         "-r", "vip.ve.atsign.zone:64",
                         "-p", "app",
                         "-d", "device-" + System.currentTimeMillis(),
                         "-n", "ns:rw",
                         "-k", appKeys.getAbsolutePath(),
                         "-s", otp);

    assertExecuteSuccess(
                         "revoke",
                         "-a", "@srie",
                         "-r", "vip.ve.atsign.zone:64",
                         "-k", keys.getAbsolutePath(),
                         "-i", getEnrollmentIdWhenAvailable(appKeys));

    assertExecuteSuccess(
                         "unrevoke",
                         "-a", "@srie",
                         "-r", "vip.ve.atsign.zone:64",
                         "-k", keys.getAbsolutePath(),
                         "-i", getEnrollmentIdWhenAvailable(appKeys));

  }


  private String getEnrollmentIdWhenAvailable(File keys) {
    try {
      if (keys.exists()) {
        return KeysUtil.loadKeys(keys).getEnrollmentId().toString();
      }
    } catch (Exception e) {
    }
    return null;
  }

  private void testEnrollListAndRespond(String atSign, File keys, String action, Supplier<String> id) {
    await().atMost(5, SECONDS).until(() -> id.get() != null);

    assertExecuteSuccess(
                         "list",
                         "-a", atSign,
                         "-r", "vip.ve.atsign.zone:64",
                         "-k", keys.getAbsolutePath(),
                         "-es", "pending");
    assertBufferFind(outBuffer, id.get());

    assertExecuteSuccess(
                         action,
                         "-a", atSign,
                         "-r", "vip.ve.atsign.zone:64",
                         "-k", keys.getAbsolutePath(),
                         "-i", id.get());
  }

  private static void sleep(long duration, TimeUnit unit) {
    try {
      Thread.sleep(unit.toMillis(duration));
    } catch (InterruptedException e) {
      throw new RuntimeException(e);
    }
  }

  private void assertExecuteSuccess(String... args) {
    assertThat(testExecute(args), equalTo(0));
  }

  private void assertExecuteNotSuccess(String... args) {
    assertThat(testExecute(args), not(equalTo(0)));
  }

  private int testExecute(String... args) {
    System.out.print(outBuffer);
    System.err.print(errBuffer);
    outBuffer.reset();
    errBuffer.reset();
    exitCode = Activate.execute(args);
    return exitCode;
  }

  private static void assertBufferAndReset(ByteArrayOutputStream buffer, org.hamcrest.Matcher<String> matcher) {
    assertThat(buffer.toString(), matcher);
    buffer.reset();
  }

  private static String assertBufferFind(ByteArrayOutputStream buffer, String regex) {
    Matcher matcher = Pattern.compile(regex, Pattern.MULTILINE).matcher(buffer.toString());
    if (matcher.find()) {
      buffer.reset();
      return matcher.groupCount() == 1 ? matcher.group(1) : matcher.group();
    } else {
      throw new AssertionError("expected " + matcher.pattern().pattern());
    }
  }

}
