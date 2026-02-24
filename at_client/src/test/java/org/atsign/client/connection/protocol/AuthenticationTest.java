package org.atsign.client.connection.protocol;

import static org.atsign.client.util.EncryptionUtil.generateRSAKeyPair;
import static org.atsign.client.util.EnrollmentId.createEnrollmentId;
import static org.atsign.common.AtSign.createAtSign;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.atsign.client.api.AtKeys;
import org.atsign.client.connection.api.AtClientConnection;
import org.atsign.common.exceptions.AtOnReadyException;
import org.atsign.common.exceptions.AtUnauthenticatedException;
import org.junit.jupiter.api.Test;

public class AuthenticationTest {

  @Test
  public void testAuthenticateWithCramDoesNotThrowException() throws Exception {
    AtClientConnection conn = TestConnectionBuilder.builder()
        .stub("from:@alice", "data:challenge")
        .stub("cram:7e91508d5.+", "data:success")
        .build();

    Authentication.authenticateWithCram(conn, createAtSign("@alice"), "secret");
  }

  @Test
  public void testAuthenticateWithCramFailThrowsExpectedException() throws Exception {
    AtClientConnection conn = TestConnectionBuilder.builder()
        .stub("from:@alice", "data:challenge")
        .stub("cram:.+", "error:AT0401:deliberate")
        .build();

    Exception ex = assertThrows(AtUnauthenticatedException.class,
                                () -> Authentication.authenticateWithCram(conn, createAtSign("@alice"), "secret"));
    assertThat(ex.getMessage(), containsString("deliberate"));
  }

  @Test
  public void testAuthenticateWithPkamDoesNotThrowException() throws Exception {
    AtKeys keys = AtKeys.builder().apkamKeyPair(generateRSAKeyPair()).build();
    AtClientConnection conn = TestConnectionBuilder.builder()
        .stub("from:@alice", "data:challenge")
        .stub("pkam:[^{].+", "data:success")
        .build();

    Authentication.authenticateWithPkam(conn, createAtSign("@alice"), keys);
  }

  @Test
  public void testAuthenticateWithApkamWithEnrollmentId() throws Exception {
    AtKeys keys = AtKeys.builder().apkamKeyPair(generateRSAKeyPair()).enrollmentId(createEnrollmentId("12345")).build();
    AtClientConnection conn = TestConnectionBuilder.builder()
        .stub("from:@alice", "data:challenge")
        .stub("pkam:signingAlgo:rsa2048:hashingAlgo:sha256:enrollmentId:12345:.+", "data:success")
        .build();

    Authentication.authenticateWithPkam(conn, createAtSign("@alice"), keys);
  }

  @Test
  public void testAuthenticateWithApkamFailThrowsExpectedException() throws Exception {
    AtKeys keys = AtKeys.builder().apkamKeyPair(generateRSAKeyPair()).enrollmentId(createEnrollmentId("12345")).build();
    AtClientConnection conn = TestConnectionBuilder.builder()
        .stub("from:@alice", "data:challenge")
        .stub("pkam:.+", "error:AT0401:deliberate")
        .build();

    Exception ex = assertThrows(AtUnauthenticatedException.class,
                                () -> Authentication.authenticateWithPkam(conn, createAtSign("@alice"), keys));
    assertThat(ex.getMessage(), containsString("deliberate"));
  }

  @Test
  public void testPkamAuthenticatorThrowsOnReadyException() throws Exception {
    AtKeys keys = AtKeys.builder().apkamKeyPair(generateRSAKeyPair()).enrollmentId(createEnrollmentId("12345")).build();
    AtClientConnection conn = TestConnectionBuilder.builder()
        .stub("from:@alice", "data:challenge")
        .stub("pkam:.+", "error:AT0401:deliberate")
        .build();

    Exception ex = assertThrows(Exception.class,
                                () -> Authentication.pkamAuthenticator(createAtSign("@alice"), keys).accept(conn));
    assertThat(ex, instanceOf(AtOnReadyException.class));
    assertThat(ex.getMessage(), containsString("deliberate"));
  }

  @Test
  public void testBytesToHex() throws Exception {
    byte[] bytes = new byte[] {(byte) 0x00, (byte) 0x0f, (byte) 0xff};
    assertThat(Authentication.bytesToHex(bytes), is("000fff"));
  }
}
