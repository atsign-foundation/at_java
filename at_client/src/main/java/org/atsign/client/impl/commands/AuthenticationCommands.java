package org.atsign.client.impl.commands;

import static org.atsign.client.impl.commands.AtExceptions.throwOnReadyException;
import static org.atsign.client.impl.commands.DataResponses.matchDataStringNoWhitespace;
import static org.atsign.client.impl.commands.DataResponses.matchDataSuccess;
import static org.atsign.client.impl.commands.ErrorResponses.throwExceptionIfError;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;

import org.atsign.client.api.AtKeys;
import org.atsign.client.api.AtCommandExecutor;
import org.atsign.client.impl.util.EncryptionUtils;
import org.atsign.client.impl.exceptions.AtException;
import org.atsign.client.api.AtSign;
import org.atsign.client.impl.exceptions.AtEncryptionException;
import org.atsign.client.impl.exceptions.AtUnauthenticatedException;

/**
 * Utility methods for handling authentication within the At Protocol
 */
public class AuthenticationCommands {

  public static Consumer<AtCommandExecutor> pkamAuthenticator(AtSign atSign, AtKeys keys) {
    return throwOnReadyException(executor -> authenticateWithPkam(executor, atSign, keys));
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
    try {

      // send a from command and expect to receive a challenge
      String fromCommand = CommandBuilders.fromCommandBuilder().atSign(atSign).build();
      String fromResponse = executor.sendSync(fromCommand);
      String challenge = matchDataStringNoWhitespace(throwExceptionIfError(fromResponse));

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

      // send a from command and expect to receive a challenge
      String fromCommand = CommandBuilders.fromCommandBuilder().atSign(atSign).build();
      String fromResponse = executor.sendSync(fromCommand);
      String challenge = matchDataStringNoWhitespace(throwExceptionIfError(fromResponse));

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

  private static final char[] HEX_ARRAY = "0123456789abcdef".toCharArray();

  public static String bytesToHex(byte[] bytes) {
    char[] hexChars = new char[bytes.length * 2];
    for (int j = 0; j < bytes.length; j++) {
      int v = bytes[j] & 0xFF;
      hexChars[j * 2] = HEX_ARRAY[v >>> 4];
      hexChars[j * 2 + 1] = HEX_ARRAY[v & 0x0F];
    }
    return new String(hexChars);
  }
}
