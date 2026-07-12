package org.atsign.client.impl.commands;

import static org.atsign.client.impl.commands.AtExceptions.throwOnReadyException;
import static org.atsign.client.impl.commands.DataResponses.matchDataStringNoWhitespace;
import static org.atsign.client.impl.commands.DataResponses.matchDataSuccess;
import static org.atsign.client.impl.commands.ErrorResponses.throwExceptionIfError;
import static org.atsign.client.impl.util.EncryptionUtils.bytesToHex;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;

import org.atsign.client.api.AtKeys;
import org.atsign.client.api.AtCommandExecutor;
import org.atsign.client.api.AtCommandExecutorContext;
import org.atsign.client.impl.util.EncryptionUtils;
import org.atsign.client.impl.exceptions.AtException;
import org.atsign.client.api.AtSign;
import org.atsign.client.impl.exceptions.AtEncryptionException;
import org.atsign.client.impl.exceptions.AtUnauthenticatedException;

/**
 * Utility methods for handling authentication within the At Protocol
 */
public class AuthenticationCommands {

  public static Consumer<AtCommandExecutor> pkamAuthenticator(AtSign atSign, AtKeys keys, Map<String, Object> config) {
    return throwOnReadyException(executor -> authenticateWithPkam(executor, atSign, keys, config));
  }

  /**
   * Returns an onReady consumer that authenticates using the identity carried by the executor's
   * {@link AtCommandExecutorContext context} — the atSign, keys and config it was built with. This
   * is the standard client path, where that identity is fixed for the connection. Onboarding, whose
   * keys are generated mid-flow, uses {@link #pkamAuthenticator(AtSign, AtKeys, Map)} with explicit
   * keys instead.
   *
   * @return an onReady consumer performing PKAM authentication from the executor's context
   */
  public static Consumer<AtCommandExecutor> pkamAuthenticator() {
    return throwOnReadyException(AuthenticationCommands::authenticateWithPkam);
  }

  /**
   * Implements the protocol workflow / sequence for PKAM authentication, taking the atSign, keys and
   * config from the executor's {@link AtCommandExecutorContext context}.
   *
   * @param executor The executor with which to send the commands; its context supplies the identity.
   * @throws AtException If authentication fails.
   */
  public static void authenticateWithPkam(AtCommandExecutor executor) throws AtException {
    AtCommandExecutorContext context = executor.getContext();
    authenticateWithPkam(executor, context.getAtSign(), context.getKeys(), context.getConfig());
  }

  /**
   * Implements the protocol workflow / sequence for PKAM authentication.
   *
   * @param executor The executor with which to send the commands.
   * @param atSign The asign to authenticate.
   * @param keys The keys to use to authenticate.
   * @throws AtException If authentication fails.
   */
  public static void authenticateWithPkam(AtCommandExecutor executor, AtSign atSign, AtKeys keys)
      throws AtException {
    authenticateWithPkam(executor, atSign, keys, null);
  }

  /**
   * Implements the protocol workflow / sequence for PKAM authentication.
   *
   * @param executor The executor with which to send the commands.
   * @param atSign The asign to authenticate.
   * @param keys The keys to use to authenticate.
   * @param config The map of configuration values to send in the from command.
   * @throws AtException If authentication fails.
   */
  public static void authenticateWithPkam(AtCommandExecutor executor,
                                          AtSign atSign,
                                          AtKeys keys,
                                          Map<String, Object> config)
      throws AtException {
    try {

      // reuse the challenge from the initial from: if the executor already sent one, otherwise
      // send a from command and expect to receive a challenge
      String challenge = consumeFromChallenge(executor);
      if (challenge == null) {
        String fromCommand = CommandBuilders.fromCommandBuilder().atSign(atSign).config(config).build();
        String fromResponse = executor.sendSync(fromCommand);
        challenge = matchDataStringNoWhitespace(throwExceptionIfError(fromResponse));
      }

      // send a pkam command with the signed challenge
      String signature = EncryptionUtils.signSHA256RSA(challenge, keys.getApkamPrivateKey());
      CommandBuilders.PkamCommandBuilder pkamCommandBuilder = CommandBuilders.pkamCommandBuilder();
      if (keys.hasEnrollmentId()) {
        pkamCommandBuilder.signingAlgo(EncryptionUtils.SIGNING_ALGO_RSA);
        pkamCommandBuilder.hashingAlgo(EncryptionUtils.HASHING_ALGO_SHA256);
        pkamCommandBuilder.enrollmentId(keys.getEnrollmentId());
      }
      pkamCommandBuilder.digest(signature);
      String pkamCommand = pkamCommandBuilder.build();
      String pkamResponse = executor.sendSync(pkamCommand);

      // verify that pkam has succeeded
      matchDataSuccess(throwExceptionIfError(pkamResponse));
    } catch (RuntimeException | ExecutionException | InterruptedException e) {
      throw new AtUnauthenticatedException("PKAM command failed : " + e.getMessage());
    }
  }

  /**
   * Implements the protocol workflow / sequence for CRAM authentication.
   *
   * @param executor The executor with which to send the commands.
   * @param atSign The asign to authenticate.
   * @param cramSecret The cramSecret that was assigned during At Server provisioning.
   * @throws AtException If authentication fails.
   */
  public static void authenticateWithCram(AtCommandExecutor executor, AtSign atSign, String cramSecret)
      throws AtException {
    try {

      // reuse the challenge from the initial from: if the executor already sent one, otherwise
      // send a from command and expect to receive a challenge
      String challenge = consumeFromChallenge(executor);
      if (challenge == null) {
        String fromCommand = CommandBuilders.fromCommandBuilder().atSign(atSign).build();
        String fromResponse = executor.sendSync(fromCommand);
        challenge = matchDataStringNoWhitespace(throwExceptionIfError(fromResponse));
      }

      // send a cram command
      String cramDigest = createDigest(cramSecret, challenge);
      String cramCommand = CommandBuilders.cramCommandBuilder().digest(cramDigest).build();
      String cramResponse = executor.sendSync(cramCommand);

      // verify that cram succeeded
      matchDataSuccess(throwExceptionIfError(cramResponse));

    } catch (RuntimeException | ExecutionException | InterruptedException e) {
      throw new AtUnauthenticatedException("CRAM command failed : " + e.getMessage());
    }
  }

  private static String createDigest(String cramSecret, String challenge) throws AtEncryptionException {
    try {
      String digestInput = cramSecret + challenge;
      byte[] digestInputBytes = digestInput.getBytes(StandardCharsets.UTF_8);
      byte[] digest = MessageDigest.getInstance("SHA-512").digest(digestInputBytes);
      return bytesToHex(digest);
    } catch (RuntimeException | NoSuchAlgorithmException e) {
      throw new AtEncryptionException("failed to generate cramDigest", e);
    }
  }

  /**
   * Returns and clears the single-use {@code from:} challenge the executor retained after issuing its
   * initial {@code from:}, or {@code null} if none is available. A {@code null} context is treated as
   * "no retained challenge" so authentication falls back to sending its own {@code from:} — the same
   * behaviour the interface's {@code getContext()} default (an EMPTY context) gives, kept null-safe
   * so test doubles that leave {@code getContext()} unstubbed behave as they did before the context
   * replaced the former {@code getFromChallenge()} accessor.
   */
  private static String consumeFromChallenge(AtCommandExecutor executor) {
    AtCommandExecutorContext context = executor.getContext();
    return context == null ? null : context.consumeChallenge();
  }
}
