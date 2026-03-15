package org.atsign.client.impl.commands;

import static org.atsign.client.impl.util.EncryptionUtils.generateRSAKeyPair;
import static org.atsign.client.impl.common.EnrollmentId.createEnrollmentId;
import static org.atsign.client.api.AtSign.createAtSign;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.atsign.client.api.AtKeys;
import org.atsign.client.api.AtCommandExecutor;
import org.atsign.client.impl.exceptions.AtOnReadyException;
import org.atsign.client.impl.exceptions.AtUnauthenticatedException;
import org.junit.jupiter.api.Test;

public class AuthenticationCommandsTest {

  @Test
  public void testAuthenticateWithCramDoesNotThrowException() throws Exception {
    AtCommandExecutor executor = TestConnectionBuilder.builder()
        .stub("from:@alice", "data:challenge")
        .stub("cram:7e91508d5.+", "data:success")
        .build();

    AuthenticationCommands.authenticateWithCram(executor, createAtSign("@alice"), "secret");
  }

  @Test
  public void testAuthenticateWithCramFailThrowsExpectedException() throws Exception {
    AtCommandExecutor executor = TestConnectionBuilder.builder()
        .stub("from:@alice", "data:challenge")
        .stub("cram:.+", "error:AT0401:deliberate")
        .build();

    Exception ex = assertThrows(AtUnauthenticatedException.class,
                                () -> AuthenticationCommands.authenticateWithCram(executor, createAtSign("@alice"),
                                                                                  "secret"));
    assertThat(ex.getMessage(), containsString("deliberate"));
  }

  @Test
  public void testAuthenticateWithPkamDoesNotThrowException() throws Exception {
    AtKeys keys = AtKeys.builder().apkamKeyPair(generateRSAKeyPair()).build();
    AtCommandExecutor executor = TestConnectionBuilder.builder()
        .stub("from:@alice", "data:challenge")
        .stub("pkam:[^{].+", "data:success")
        .build();

    AuthenticationCommands.authenticateWithPkam(executor, createAtSign("@alice"), keys);
  }

  @Test
  public void testAuthenticateWithApkamWithEnrollmentId() throws Exception {
    AtKeys keys = AtKeys.builder().apkamKeyPair(generateRSAKeyPair()).enrollmentId(createEnrollmentId("12345")).build();
    AtCommandExecutor executor = TestConnectionBuilder.builder()
        .stub("from:@alice", "data:challenge")
        .stub("pkam:signingAlgo:rsa2048:hashingAlgo:sha256:enrollmentId:12345:.+", "data:success")
        .build();

    AuthenticationCommands.authenticateWithPkam(executor, createAtSign("@alice"), keys);
  }

  @Test
  public void testAuthenticateWithApkamFailThrowsExpectedException() throws Exception {
    AtKeys keys = AtKeys.builder().apkamKeyPair(generateRSAKeyPair()).enrollmentId(createEnrollmentId("12345")).build();
    AtCommandExecutor executor = TestConnectionBuilder.builder()
        .stub("from:@alice", "data:challenge")
        .stub("pkam:.+", "error:AT0401:deliberate")
        .build();

    Exception ex = assertThrows(AtUnauthenticatedException.class,
                                () -> AuthenticationCommands.authenticateWithPkam(executor, createAtSign("@alice"),
                                                                                  keys));
    assertThat(ex.getMessage(), containsString("deliberate"));
  }

  @Test
  public void testPkamAuthenticatorThrowsOnReadyException() throws Exception {
    AtKeys keys = AtKeys.builder().apkamKeyPair(generateRSAKeyPair()).enrollmentId(createEnrollmentId("12345")).build();
    AtCommandExecutor executor = TestConnectionBuilder.builder()
        .stub("from:@alice", "data:challenge")
        .stub("pkam:.+", "error:AT0401:deliberate")
        .build();

    Exception ex = assertThrows(Exception.class,
                                () -> AuthenticationCommands.pkamAuthenticator(createAtSign("@alice"), keys)
                                    .accept(executor));
    assertThat(ex, instanceOf(AtOnReadyException.class));
    assertThat(ex.getMessage(), containsString("deliberate"));
  }

  @Test
  public void testBytesToHex() throws Exception {
    byte[] bytes = new byte[] {(byte) 0x00, (byte) 0x0f, (byte) 0xff};
    assertThat(AuthenticationCommands.bytesToHex(bytes), is("000fff"));
  }
}
