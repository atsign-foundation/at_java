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

  /**
   * Returns an {@code onReady} consumer that issues {@code from:@atSign} as the first command on the
   * connection and retains the challenge from the response in the given {@code context}. This
   * establishes the connection's atSign up front so that proxies / gateways can route it, and lets
   * any authentication that follows on the same connection reuse the challenge (see
   * {@link #pkamAuthenticator(AtCommandExecutorContext)}) rather than issuing a second {@code from:}.
   *
   * @param context the connection context; supplies the atSign / config and receives the challenge
   * @return an onReady consumer that sends the initial {@code from:}
   */
  public static Consumer<AtCommandExecutor> sendFrom(AtCommandExecutorContext context) {
    return throwOnReadyException(executor -> {
      String fromCommand = CommandBuilders.fromCommandBuilder()
          .atSign(context.getAtSign())
          .config(context.getConfig())
          .build();
      String fromResponse = executor.sendSync(fromCommand);
      context.setChallenge(matchDataStringNoWhitespace(throwExceptionIfError(fromResponse)));
    });
  }

  /**
   * Returns an {@code onReady} consumer that authenticates with PKAM using the identity in the given
   * {@code context}, reusing the challenge from an initial {@code from:} (as issued by
   * {@link #sendFrom(AtCommandExecutorContext)}) when one is available and otherwise issuing its own.
   * This is the standard client path, where the identity is fixed for the connection.
   *
   * @param context the connection context; supplies the atSign / keys / config and the challenge
   * @return an onReady consumer that performs PKAM authentication
   */
  public static Consumer<AtCommandExecutor> pkamAuthenticator(AtCommandExecutorContext context) {
    return throwOnReadyException(executor -> authenticateWithPkam(executor, context.getAtSign(),
                                                                  context.getKeys(), context.getConfig(),
                                                                  context.consumeChallenge()));
  }

  public static Consumer<AtCommandExecutor> pkamAuthenticator(AtSign atSign, AtKeys keys, Map<String, Object> config) {
    return throwOnReadyException(executor -> authenticateWithPkam(executor, atSign, keys, config));
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
    authenticateWithPkam(executor, atSign, keys, config, null);
  }

  /**
   * Implements the protocol workflow / sequence for PKAM authentication, reusing an already-issued
   * {@code from:} challenge when one is supplied.
   *
   * @param executor The executor with which to send the commands.
   * @param atSign The asign to authenticate.
   * @param keys The keys to use to authenticate.
   * @param config The map of configuration values to send in the from command.
   * @param reusableChallenge The challenge from an initial {@code from:} to reuse, or {@code null} to
   *        issue a fresh {@code from:}.
   * @throws AtException If authentication fails.
   */
  private static void authenticateWithPkam(AtCommandExecutor executor,
                                           AtSign atSign,
                                           AtKeys keys,
                                           Map<String, Object> config,
                                           String reusableChallenge)
      throws AtException {
    try {

      // reuse the challenge from the initial from: if one was issued on this connection, otherwise
      // send a from command and expect to receive a challenge
      String challenge = reusableChallenge;
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
   * Implements the protocol workflow / sequence for CRAM authentication, reusing the challenge from
   * an initial {@code from:} (as issued by {@link #sendFrom(AtCommandExecutorContext)}) held in the
   * given {@code context} when one is available and otherwise issuing its own. Used by onboarding,
   * which issues a {@code from:} on connect, runs a connectivity scan, then authenticates.
   *
   * @param executor The executor with which to send the commands.
   * @param context The connection context; supplies the atSign and the retained challenge.
   * @param cramSecret The cramSecret that was assigned during At Server provisioning.
   * @throws AtException If authentication fails.
   */
  public static void authenticateWithCram(AtCommandExecutor executor,
                                          AtCommandExecutorContext context,
                                          String cramSecret)
      throws AtException {
    try {

      // reuse the challenge from the initial from: if one was issued on this connection, otherwise
      // send a from command and expect to receive a challenge
      String challenge = context.consumeChallenge();
      if (challenge == null) {
        String fromCommand = CommandBuilders.fromCommandBuilder().atSign(context.getAtSign()).build();
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
}
