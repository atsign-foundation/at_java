package org.atsign.client.impl.cli;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.security.SecureRandom;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.atsign.client.impl.util.KeysUtils;
import org.atsign.cucumber.helpers.Helpers;
import org.atsign.virtualenv.VirtualEnv;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.github.stefanbirkner.systemlambda.Statement;
import com.github.stefanbirkner.systemlambda.SystemLambda;

class CliIT {

  public static final String VIRTUAL_ENV_ROOT = "vip.ve.atsign.zone:64";
  public static String AT_SIGN_KEYS_DIR;
  public static String ATSIGN_KEYS_SUFFIX;

  @BeforeAll
  public static void classSetup() {
    if (!Helpers.isHostPortReachable(VIRTUAL_ENV_ROOT, SECONDS.toMillis(2))) {
      VirtualEnv.setUp();
    }
    AT_SIGN_KEYS_DIR = KeysUtils.expectedKeysFilesLocation;
    ATSIGN_KEYS_SUFFIX = KeysUtils.keysFileSuffix;
    KeysUtils.expectedKeysFilesLocation = "target/at_demo_data/lib/assets/atkeys";
    KeysUtils.keysFileSuffix = ".atKeys";
  }

  @AfterAll
  public static void classTeardown() {
    KeysUtils.expectedKeysFilesLocation = AT_SIGN_KEYS_DIR;
    KeysUtils.keysFileSuffix = ATSIGN_KEYS_SUFFIX;
  }

  @Test
  public void testShareUsage() throws Exception {
    int shareCode = runMainExpectSystemExit(() -> Share.main(new String[] {VIRTUAL_ENV_ROOT}));
    assertThat(shareCode, not(equalTo(0)));

    int scanCode = runMainExpectSystemExit(() -> Scan.main(new String[] {VIRTUAL_ENV_ROOT}));
    assertThat(scanCode, not(equalTo(0)));

    int getCode = runMainExpectSystemExit(() -> Get.main(new String[] {VIRTUAL_ENV_ROOT}));
    assertThat(getCode, not(equalTo(0)));

    int deleteCode = runMainExpectSystemExit(() -> Delete.main(new String[] {VIRTUAL_ENV_ROOT}));
    assertThat(deleteCode, not(equalTo(0)));
  }

  @Test
  public void testDumpKeys() throws Exception {
    List<String> stdout = runMain(() -> DumpKeys.main(new String[] {"colin"}));
    findLines(stdout,
              "\\s+key: aesPkamPublicKey", "\\s+value:\\s+\\S+",
              "\\s+key: aesPkamPrivateKey", "\\s+value:\\s+\\S+");

  }

  @Test
  public void testShareScanGetDelete() throws Exception {

    String keyname = randomId(6);

    // create a new key value

    String[] shareArgs = {VIRTUAL_ENV_ROOT, "colin", "jeremy", keyname, "mary had a little lamb"};
    List<String> sharedStdout = runMain(() -> Share.main(shareArgs));

    assertThat(sharedStdout, is(empty()));

    // scan for new key value

    String[] scanArgs = {VIRTUAL_ENV_ROOT, "colin", keyname};
    List<String> scanStdOut = runMain(() -> Scan.main(scanArgs), "l", "0", "q");

    scanStdOut = findLines(scanStdOut, "Enter index you want to llookup.+");
    scanStdOut = findLines(scanStdOut, "\\s+0:\\s+.*" + keyname + ".*");
    findLines(scanStdOut,
              "Full KeyName: @jeremy:" + keyname + "@colin",
              "KeyName: " + keyname,
              "SharedBy: @colin",
              "SharedWith: @jeremy",
              "KeyType: SharedKey");

    // get the key value as the at sign which the key is shared with

    String[] getArgs = {VIRTUAL_ENV_ROOT, "jeremy", "colin", keyname};
    List<String> getStdout = runMain(() -> Get.main(getArgs));
    findLines(getStdout, "get response : mary had a little lamb");

    // delete the key value

    String[] deleteArgs = {VIRTUAL_ENV_ROOT, "colin", "jeremy", keyname};
    List<String> deleteStdout = runMain(() -> Delete.main(deleteArgs));
    assertThat(deleteStdout, is(empty()));

    // verify it's gone

    List<String> secondScanStdout = runMain(() -> Scan.main(scanArgs), "l", "q");
    assertThrows(AssertionError.class, () -> findLines(secondScanStdout, "\\s+0:\\s+.*" + keyname + ".*"));
  }

  public static int runMainExpectSystemExit(Statement statement, String... stdin) throws Exception {
    AtomicReference<String> stdout = new AtomicReference<>();
    return SystemLambda.catchSystemExit(() -> stdout.set(SystemLambda.tapSystemErr(() -> {
      SystemLambda.SystemInStub stub = SystemLambda.withTextFromSystemIn(String.join("\n", List.of(stdin)));
      stub.execute(statement);
    })));
  }

  public static List<String> runMain(Statement statement, String... stdin) throws Exception {
    AtomicReference<String> stdout = new AtomicReference<>();
    stdout.set(SystemLambda.tapSystemOut(() -> {
      SystemLambda.SystemInStub stub = SystemLambda.withTextFromSystemIn(String.join("\n", List.of(stdin)));
      stub.execute(statement);
    }));
    return stdout.get().lines().collect(Collectors.toList());
  }

  public static List<String> findLines(List<String> lines, String... regexes) {
    List<String> result = lines;
    for (String regex : regexes) {
      result = findLine(result, Pattern.compile(regex).matcher(""));
    }
    return result;
  }

  public static List<String> findLine(List<String> lines, Matcher matcher) {
    for (int i = 0; i < lines.size(); i++) {
      if (matcher.reset(lines.get(i)).matches()) {
        return lines.subList(i, lines.size() - 1);
      }
    }
    throw new AssertionError("unable to find line that matches : " + matcher.pattern().pattern() + " in\n" +
        String.join("\n", lines));
  }

  private static final char[] ALPHABET = "0123456789abcdefghijklmnopqrstuvwxyz".toCharArray();

  private static final SecureRandom RANDOM = new SecureRandom();

  public static String randomId(int length) {
    char[] result = new char[length];

    for (int i = 0; i < length; i++) {
      result[i] = ALPHABET[RANDOM.nextInt(ALPHABET.length)];
    }

    return new String(result);
  }
}
