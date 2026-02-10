package org.atsign.client.util;

import static org.atsign.common.VerbBuilders.*;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;

import org.atsign.client.api.AtConnection;
import org.atsign.client.api.AtKeys;
import org.atsign.client.api.impl.connections.AtSecondaryConnection;
import org.atsign.common.AtException;
import org.atsign.common.AtSign;
import org.atsign.common.exceptions.AtClientConfigException;
import org.atsign.common.exceptions.AtEncryptionException;
import org.atsign.common.exceptions.AtUnauthenticatedException;

/**
 * Encapsulates Atsign Platform authentication command response workflows.
 */
public class AuthUtil {

  /**
   * Sends a from command followed by cram command.
   *
   * @param connection connection to an AtServer
   * @param atSign the AtSign to authenticate
   * @param cramSecret the secret with which to respond to the authentication challenge
   * @throws AtException If the authentication fails
   * @throws IOException Delegated from AtServer
   */
  public void authenticateWithCram(AtSecondaryConnection connection, AtSign atSign, String cramSecret)
      throws AtException, IOException {
    String fromResponse = connection.executeCommand(fromCommandBuilder().atSign(atSign).build());
    if (!fromResponse.startsWith("data:")) {
      throw new AtUnauthenticatedException("Invalid response to 'from': " + fromResponse);
    }

    String challenge = fromResponse.replaceFirst("data:", "");
    String cramDigest;
    try {
      cramDigest = _getCramDigest(cramSecret, challenge);
    } catch (NoSuchAlgorithmException e) {
      throw new AtEncryptionException("Failed to generate cramDigest", e);
    }

    String cramResponse = connection.executeCommand(cramCommandBuilder().digest(cramDigest).build());
    if (!cramResponse.startsWith("data:success")) {
      throw new AtUnauthenticatedException("CRAM command failed: " + cramResponse);
    }
  }

  /**
   * Sends a from command followed by a pkam command.
   *
   * @param connection connection to an AtServer
   * @param atSign the AtSign to authenticate
   * @param keys AtKeys which contains the PKAM private key and associated {@link EnrollmentId}
   * @throws AtException If the authentication fails
   * @throws IOException Delegated from AtServer
   */
  public void authenticateWithPkam(AtConnection connection, AtSign atSign, AtKeys keys)
      throws AtException, IOException {
    if (!keys.hasPkamKey()) {
      throw new AtClientConfigException("Cannot authenticate with PKAM: Keys file does not contain PKAM keys");
    }

    String fromResponse = connection.executeCommand(fromCommandBuilder().atSign(atSign).build());

    String dataPrefix = "data:";
    if (!fromResponse.startsWith(dataPrefix)) {
      throw new AtUnauthenticatedException("Invalid response to 'from' command: " + fromResponse);
    }
    fromResponse = fromResponse.substring(dataPrefix.length());

    PrivateKey privateKey;
    try {
      privateKey = EncryptionUtil._privateKeyFromBase64(keys.getApkamPrivateKey());
    } catch (Exception e) {
      throw new AtClientConfigException("Failed to get private key from stored string");
    }

    String signature;
    try {
      signature = EncryptionUtil._signSHA256RSA(fromResponse, privateKey);
    } catch (Exception e) {
      throw new AtEncryptionException("Failed to create SHA256 signature");
    }

    PkamCommandBuilder builder = pkamCommandBuilder();
    if (keys.hasEnrollmentId()) {
      builder.signingAlgo(EncryptionUtil.SIGNING_ALGO_RSA);
      builder.hashingAlgo(EncryptionUtil.HASHING_ALGO_SHA256);
      builder.enrollmentId(keys.getEnrollmentId());
    }
    builder.digest(signature);

    String pkamResponse = connection.executeCommand(builder.build());

    if (!pkamResponse.startsWith("data:success")) {
      throw new AtUnauthenticatedException("PKAM command failed: " + pkamResponse);
    }
  }

  private String _getCramDigest(String cramSecret, String challenge) throws NoSuchAlgorithmException {
    String digestInput = cramSecret + challenge;

    byte[] digestInputBytes = digestInput.getBytes(StandardCharsets.UTF_8);
    byte[] digest = MessageDigest.getInstance("SHA-512").digest(digestInputBytes);

    return bytesToHex(digest);
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
