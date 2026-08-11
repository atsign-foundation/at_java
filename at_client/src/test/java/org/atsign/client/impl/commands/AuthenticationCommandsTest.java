package org.atsign.client.impl.commands;

import org.atsign.client.api.AtCommandExecutor;
import org.atsign.client.api.AtCommandExecutorContext;
import org.atsign.client.api.AtKeys;
import org.atsign.client.impl.exceptions.AtOnReadyException;
import org.atsign.client.impl.exceptions.AtUnauthenticatedException;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.Map;

import static org.atsign.client.impl.util.EncryptionUtils.generateRSAKeyPair;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.matches;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import org.atsign.client.api.AtSign;
import org.atsign.client.api.EnrollmentId;

public class AuthenticationCommandsTest {

  @Test
  public void testAuthenticateWithCramDoesNotThrowException() throws Exception {
    AtCommandExecutor executor = TestExecutorBuilder.builder()
        .stub("from:@alice", "data:challenge")
        .stub("cram:7e91508d5.+", "data:success")
        .build();

    AuthenticationCommands.authenticateWithCram(executor,
                                                new AtCommandExecutorContext(AtSign.of("@alice"), null, null),
                                                "secret");
  }

  @Test
  public void testAuthenticateWithCramFailThrowsExpectedException() throws Exception {
    AtCommandExecutor executor = TestExecutorBuilder.builder()
        .stub("from:@alice", "data:challenge")
        .stub("cram:.+", "error:AT0401:deliberate")
        .build();

    Exception ex = assertThrows(AtUnauthenticatedException.class,
                                () -> AuthenticationCommands.authenticateWithCram(
                                                                                  executor,
                                                                                  new AtCommandExecutorContext(
                                                                                      AtSign.of("@alice"), null,
                                                                                      null),
                                                                                  "secret"));
    assertThat(ex.getMessage(), containsString("deliberate"));
  }

  @Test
  public void testAuthenticateWithPkamDoesNotThrowException() throws Exception {
    AtKeys keys = AtKeys.builder().apkamKeyPair(generateRSAKeyPair()).build();
    AtCommandExecutor executor = TestExecutorBuilder.builder()
        .stub("from:@alice", "data:challenge")
        .stub("pkam:[^{].+", "data:success")
        .build();

    AuthenticationCommands.authenticateWithPkam(executor, AtSign.of("@alice"), keys);
  }

  @Test
  public void testAuthenticateWithApkamWithEnrollmentId() throws Exception {
    AtKeys keys = AtKeys.builder().apkamKeyPair(generateRSAKeyPair()).enrollmentId(EnrollmentId.of("12345")).build();
    AtCommandExecutor executor = TestExecutorBuilder.builder()
        .stub("from:@alice", "data:challenge")
        .stub("pkam:signingAlgo:rsa2048:hashingAlgo:sha256:enrollmentId:12345:.+", "data:success")
        .build();

    AuthenticationCommands.authenticateWithPkam(executor, AtSign.of("@alice"), keys);
  }

  @Test
  public void testAuthenticateWithApkamWithConfig() throws Exception {
    AtKeys keys = AtKeys.builder().apkamKeyPair(generateRSAKeyPair()).enrollmentId(EnrollmentId.of("12345")).build();
    AtCommandExecutor executor = TestExecutorBuilder.builder()
        .stub("from:@alice:clientConfig:.+", "data:challenge")
        .stub("pkam:signingAlgo:rsa2048:hashingAlgo:sha256:enrollmentId:12345:.+", "data:success")
        .build();

    Map<String, Object> config = Collections.singletonMap("clientVersion", "1.2.3");
    AuthenticationCommands.authenticateWithPkam(executor, AtSign.of("@alice"), keys, config);
  }

  @Test
  public void testAuthenticateWithApkamFailThrowsExpectedException() throws Exception {
    AtKeys keys = AtKeys.builder().apkamKeyPair(generateRSAKeyPair()).enrollmentId(EnrollmentId.of("12345")).build();
    AtCommandExecutor executor = TestExecutorBuilder.builder()
        .stub("from:@alice", "data:challenge")
        .stub("pkam:.+", "error:AT0401:deliberate")
        .build();

    Exception ex = assertThrows(AtUnauthenticatedException.class,
                                () -> AuthenticationCommands.authenticateWithPkam(executor, AtSign.of("@alice"),
                                                                                  keys));
    assertThat(ex.getMessage(), containsString("deliberate"));
  }

  @Test
  public void testPkamAuthenticatorThrowsOnReadyException() throws Exception {
    AtKeys keys = AtKeys.builder().apkamKeyPair(generateRSAKeyPair()).enrollmentId(EnrollmentId.of("12345")).build();
    AtCommandExecutor executor = TestExecutorBuilder.builder()
        .stub("from:@alice", "data:challenge")
        .stub("pkam:.+", "error:AT0401:deliberate")
        .build();

    Exception ex = assertThrows(Exception.class,
                                () -> AuthenticationCommands.pkamAuthenticator(AtSign.of("@alice"), keys, null)
                                    .accept(executor));
    assertThat(ex, instanceOf(AtOnReadyException.class));
    assertThat(ex.getMessage(), containsString("deliberate"));
  }

  @Test
  public void testSendFromIssuesFromAndRetainsChallenge() throws Exception {
    AtCommandExecutor executor = TestExecutorBuilder.builder()
        .stub("from:@alice", "data:challenge")
        .build();
    AtCommandExecutorContext context = new AtCommandExecutorContext(AtSign.of("@alice"), null, null);

    AuthenticationCommands.sendFrom(context).accept(executor);

    verify(executor, times(1)).sendSync(matches("from:.*"));
    assertThat(context.consumeChallenge(), is("challenge"));
  }

  @Test
  public void testPkamAuthenticatorReusesTheInitialFromChallenge() throws Exception {
    AtKeys keys = AtKeys.builder().apkamKeyPair(generateRSAKeyPair()).build();
    AtCommandExecutor executor = TestExecutorBuilder.builder()
        .stub("from:@alice", "data:challenge")
        .stub("pkam:[^{].+", "data:success")
        .build();
    AtCommandExecutorContext context = new AtCommandExecutorContext(AtSign.of("@alice"), keys, null);

    // the from: sender runs first (as wired by createOnReady), then PKAM reuses its challenge
    AuthenticationCommands.sendFrom(context).accept(executor);
    AuthenticationCommands.pkamAuthenticator(context).accept(executor);

    // exactly one from: for the whole connection — PKAM did not issue a second one
    verify(executor, times(1)).sendSync(matches("from:.*"));
    verify(executor, times(1)).sendSync(matches("pkam:.*"));
    assertThat(context.consumeChallenge(), is(nullValue()));
  }

  @Test
  public void testPkamAuthenticatorIssuesItsOwnFromWhenNoChallengeRetained() throws Exception {
    AtKeys keys = AtKeys.builder().apkamKeyPair(generateRSAKeyPair()).build();
    AtCommandExecutor executor = TestExecutorBuilder.builder()
        .stub("from:@alice", "data:challenge")
        .stub("pkam:[^{].+", "data:success")
        .build();
    AtCommandExecutorContext context = new AtCommandExecutorContext(AtSign.of("@alice"), keys, null);

    // no prior sendFrom: the authenticator must issue its own from: to obtain a challenge
    AuthenticationCommands.pkamAuthenticator(context).accept(executor);

    verify(executor, times(1)).sendSync(matches("from:.*"));
    verify(executor, times(1)).sendSync(matches("pkam:.*"));
  }

  @Test
  public void testRetainedChallengeIsConsumedAtMostOnce() {
    AtCommandExecutorContext context = new AtCommandExecutorContext(AtSign.of("@alice"), null, null);
    context.setChallenge("challenge");

    // single-use: the first consumer gets it, a second (e.g. a further auth on the same connection)
    // gets null and falls back to issuing its own from:
    assertThat(context.consumeChallenge(), is("challenge"));
    assertThat(context.consumeChallenge(), is(nullValue()));
  }
}
