package org.atsign.client.connection.protocol;

import static org.atsign.client.connection.protocol.AtExceptions.throwOnReadyException;
import static org.atsign.client.connection.protocol.Data.matchDataStringNoWhitespace;
import static org.atsign.client.connection.protocol.Data.matchDataSuccess;
import static org.atsign.client.connection.protocol.Error.throwExceptionIfError;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;

import org.atsign.client.api.AtKeys;
import org.atsign.client.connection.api.AtClientConnection;
import org.atsign.client.util.EncryptionUtil;
import org.atsign.common.AtException;
import org.atsign.common.AtSign;
import org.atsign.common.VerbBuilders;
import org.atsign.common.exceptions.AtEncryptionException;
import org.atsign.common.exceptions.AtUnauthenticatedException;

/**
 * Utility methods for handling authentication within the AtSign protocol
 */
public class Authentication {

  public static Consumer<AtClientConnection> pkamAuthenticator(AtSign atSign, AtKeys keys) {
    return throwOnReadyException(connection -> authenticateWithPkam(connection, atSign, keys));
  }

  public static void authenticateWithPkam(AtClientConnection connection, AtSign atSign, AtKeys keys)
      throws AtException {
    try {

      // send a from command and expect to receive a challenge
      String fromCommand = VerbBuilders.fromCommandBuilder().atSign(atSign).build();
      String fromResponse = connection.sendSync(fromCommand);
      String challenge = matchDataStringNoWhitespace(throwExceptionIfError(fromResponse));

      // send a pkam command with the signed challenge
      String signature = EncryptionUtil.signSHA256RSA(challenge, keys.getApkamPrivateKey());
      VerbBuilders.PkamCommandBuilder pkamCommandBuilder = VerbBuilders.pkamCommandBuilder();
      if (keys.hasEnrollmentId()) {
        pkamCommandBuilder.signingAlgo(EncryptionUtil.SIGNING_ALGO_RSA);
        pkamCommandBuilder.hashingAlgo(EncryptionUtil.HASHING_ALGO_SHA256);
        pkamCommandBuilder.enrollmentId(keys.getEnrollmentId());
      }
      pkamCommandBuilder.digest(signature);
      String pkamCommand = pkamCommandBuilder.build();
      String pkamResponse = connection.sendSync(pkamCommand);

      // verify that pkam has succeeded
      matchDataSuccess(throwExceptionIfError(pkamResponse));
    } catch (RuntimeException | ExecutionException | InterruptedException e) {
      throw new AtUnauthenticatedException("PKAM command failed : " + e.getMessage());
    }
  }

  public static void authenticateWithCram(AtClientConnection connection, AtSign atSign, String cramSecret)
      throws AtException {
    try {

      // send a from command and expect to receive a challenge
      String fromCommand = VerbBuilders.fromCommandBuilder().atSign(atSign).build();
      String fromResponse = connection.sendSync(fromCommand);
      String challenge = matchDataStringNoWhitespace(throwExceptionIfError(fromResponse));

      // send a cram command
      String cramDigest = createDigest(cramSecret, challenge);
      String cramCommand = VerbBuilders.cramCommandBuilder().digest(cramDigest).build();
      String cramResponse = connection.sendSync(cramCommand);

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
