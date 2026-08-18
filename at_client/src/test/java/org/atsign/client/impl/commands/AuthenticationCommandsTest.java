package org.atsign.client.impl.commands;

import org.atsign.client.api.AtCommandExecutor;
import org.atsign.client.api.AtCommandExecutorContext;
import org.atsign.client.api.AtKeys;
import org.atsign.client.impl.exceptions.AtOnReadyException;
import org.atsign.client.impl.exceptions.AtUnauthenticatedException;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.atsign.client.api.AtSign.createAtSign;
import static org.atsign.client.impl.common.EnrollmentId.createEnrollmentId;
import static org.atsign.client.impl.util.EncryptionUtils.generateRSAKeyPair;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.matches;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

public class AuthenticationCommandsTest {

  @Test
  public void testAuthenticateWithCramDoesNotThrowException() throws Exception {
    AtCommandExecutor executor = TestExecutorBuilder.builder()
        .stub("from:@alice", "data:challenge")
        .stub("cram:7e91508d5.+", "data:success")
        .build();

    AtCommandExecutorContext context = new AtCommandExecutorContext(createAtSign("@alice"), null);
    AuthenticationCommands.authenticateWithCram(executor, context, "secret");
  }

  @Test
  public void testAuthenticateWithCramFailThrowsExpectedException() throws Exception {
    AtCommandExecutor executor = TestExecutorBuilder.builder()
        .stub("from:@alice", "data:challenge")
        .stub("cram:.+", "error:AT0401:deliberate")
        .build();

    AtCommandExecutorContext context = new AtCommandExecutorContext(createAtSign("@alice"), null);
    Exception ex = assertThrows(AtUnauthenticatedException.class,
                                () -> AuthenticationCommands.authenticateWithCram(executor, context, "secret"));
    assertThat(ex.getMessage(), containsString("deliberate"));
  }

  @Test
  public void testAuthenticateWithPkamDoesNotThrowException() throws Exception {
    AtKeys keys = AtKeys.builder().apkamKeyPair(generateRSAKeyPair()).build();
    AtCommandExecutor executor = TestExecutorBuilder.builder()
        .stub("from:@alice", "data:challenge")
        .stub("pkam:[^{].+", "data:success")
        .build();

    AtCommandExecutorContext context = new AtCommandExecutorContext(createAtSign("@alice"), keys);
    AuthenticationCommands.authenticateWithPkam(executor, context);
  }

  @Test
  public void testAuthenticateWithApkamWithEnrollmentIdDoesNotThrowException() throws Exception {
    AtKeys keys = AtKeys.builder().apkamKeyPair(generateRSAKeyPair()).enrollmentId(createEnrollmentId("12345")).build();
    AtCommandExecutor executor = TestExecutorBuilder.builder()
        .stub("from:@alice", "data:challenge")
        .stub("pkam:signingAlgo:rsa2048:hashingAlgo:sha256:enrollmentId:12345:.+", "data:success")
        .build();

    AtCommandExecutorContext context = new AtCommandExecutorContext(createAtSign("@alice"), keys);
    AuthenticationCommands.authenticateWithPkam(executor, context);
  }

  @Test
  public void testAuthenticateWithApkamWithConfigDoesNotThrowException() throws Exception {
    AtKeys keys = AtKeys.builder().apkamKeyPair(generateRSAKeyPair()).enrollmentId(createEnrollmentId("12345")).build();
    AtCommandExecutor executor = TestExecutorBuilder.builder()
        .stub("from:@alice:clientConfig:.+", "data:challenge")
        .stub("pkam:signingAlgo:rsa2048:hashingAlgo:sha256:enrollmentId:12345:.+", "data:success")
        .build();

    Map<String, Object> config = Collections.singletonMap("clientVersion", "1.2.3");
    AtCommandExecutorContext context = new AtCommandExecutorContext(createAtSign("@alice"), keys, config);
    AuthenticationCommands.authenticateWithPkam(executor, context);
  }

  @Test
  public void testAuthenticateWithApkamFailThrowsExpectedException() throws Exception {
    AtKeys keys = AtKeys.builder().apkamKeyPair(generateRSAKeyPair()).enrollmentId(createEnrollmentId("12345")).build();
    AtCommandExecutor executor = TestExecutorBuilder.builder()
        .stub("from:@alice", "data:challenge")
        .stub("pkam:.+", "error:AT0401:deliberate")
        .build();
    AtCommandExecutorContext context = new AtCommandExecutorContext(createAtSign("@alice"), keys);

    Exception ex = assertThrows(AtUnauthenticatedException.class,
                                () -> AuthenticationCommands.authenticateWithPkam(executor, context));
    assertThat(ex.getMessage(), containsString("deliberate"));
  }

  @Test
  public void testPkamAuthenticatorThrowsOnReadyException() throws Exception {
    AtKeys keys = AtKeys.builder().apkamKeyPair(generateRSAKeyPair()).enrollmentId(createEnrollmentId("12345")).build();
    AtCommandExecutor executor = TestExecutorBuilder.builder()
        .stub("from:@alice", "data:challenge")
        .stub("pkam:.+", "error:AT0401:deliberate")
        .build();
    AtCommandExecutorContext context = new AtCommandExecutorContext(createAtSign("@alice"), keys);

    Exception ex = assertThrows(Exception.class,
                                () -> AuthenticationCommands.pkamAuthenticator(context).accept(executor));
    assertThat(ex, instanceOf(AtOnReadyException.class));
    assertThat(ex.getMessage(), containsString("deliberate"));
  }

  @Test
  public void testSendFromIssuesFromAndRetainsChallenge() throws Exception {
    AtCommandExecutor executor = TestExecutorBuilder.builder()
        .stub("from:@alice", "data:challenge")
        .build();
    AtCommandExecutorContext context = new AtCommandExecutorContext(createAtSign("@alice"), null);

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
    AtCommandExecutorContext context = new AtCommandExecutorContext(createAtSign("@alice"), keys);

    // the from: sender runs first (as wired by createOnReady), then PKAM reuses its challenge
    AuthenticationCommands.sendFrom(context).accept(executor);
    AuthenticationCommands.pkamAuthenticator(context).accept(executor);

    // exactly one from: for the whole connection — PKAM did not issue a second one
    verify(executor, times(1)).sendSync(matches("from:.*"));
    verify(executor, times(1)).sendSync(matches("pkam:.*"));
    assertThat(context.consumeChallenge(), is(nullValue()));
  }

  @Test
  public void testAuthenticateWithCramReusesTheInitialFromChallenge() throws Exception {
    AtCommandExecutor executor = TestExecutorBuilder.builder()
        .stub("from:@alice", "data:challenge")
        .stub("cram:7e91508d5.+", "data:success")
        .build();

    AtCommandExecutorContext context = new AtCommandExecutorContext(createAtSign("@alice"), null);

    // the from: sender runs first (as wired by createOnReady), then CRAM reuses its challenge
    AuthenticationCommands.sendFrom(context).accept(executor);
    AuthenticationCommands.authenticateWithCram(executor, context, "secret");

    // exactly one from: for the whole connection — CRAM did not issue a second one
    verify(executor, times(1)).sendSync(matches("from:.*"));
    verify(executor, times(1)).sendSync(matches("cram:.*"));
    assertThat(context.consumeChallenge(), is(nullValue()));
  }

  @Test
  public void testPkamAuthenticatorIssuesItsOwnFromWhenNoChallengeRetained() throws Exception {
    AtKeys keys = AtKeys.builder().apkamKeyPair(generateRSAKeyPair()).build();
    AtCommandExecutor executor = TestExecutorBuilder.builder()
        .stub("from:@alice", "data:challenge")
        .stub("pkam:[^{].+", "data:success")
        .build();
    AtCommandExecutorContext context = new AtCommandExecutorContext(createAtSign("@alice"), keys);

    // no prior sendFrom: the authenticator must issue its own from: to obtain a challenge
    AuthenticationCommands.pkamAuthenticator(context).accept(executor);

    verify(executor, times(1)).sendSync(matches("from:.*"));
    verify(executor, times(1)).sendSync(matches("pkam:.*"));
  }

  @Test
  public void testRetainedChallengeIsConsumedAtMostOnce() {
    AtCommandExecutorContext context = new AtCommandExecutorContext(createAtSign("@alice"), null);
    context.setChallenge("challenge");

    // single-use: the first consumer gets it, a second (e.g. a further auth on the same connection)
    // gets null and falls back to issuing its own from:
    assertThat(context.consumeChallenge(), is("challenge"));
    assertThat(context.consumeChallenge(), is(nullValue()));
  }

  @Test
  public void testFromReplacesUnconsumedChallenge() throws Exception {
    AtomicInteger invocations = new AtomicInteger();
    AtCommandExecutor executor = TestExecutorBuilder.builder()
        .stub("from:@alice", m -> "data:challenge" + invocations.incrementAndGet())
        .build();
    AtCommandExecutorContext context = new AtCommandExecutorContext(createAtSign("@alice"), null);

    AuthenticationCommands.sendFrom(context).accept(executor);
    AuthenticationCommands.sendFrom(context).accept(executor);

    assertThat(context.consumeChallenge(), equalTo("challenge2"));
  }
}
