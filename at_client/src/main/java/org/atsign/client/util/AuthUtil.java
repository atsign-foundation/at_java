package org.atsign.client.util;

import org.atsign.client.api.AtConnection;
import org.atsign.common.AtSign;
import org.atsign.client.api.impl.connections.AtSecondaryConnection;
import org.atsign.common.AtException;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.util.Map;

/**
 *
 */
public class AuthUtil {
    public void authenticateWithCram(AtSecondaryConnection connection, AtSign atSign, String cramSecret) throws AtException, IOException {
        String fromResponse = connection.executeCommand("from:" + atSign);
        if (! fromResponse.startsWith("data:")) {
            throw new AtException("CRAM command got invalid from response: " + fromResponse);
        }

        String challenge = fromResponse.replaceFirst("data:", "");
        String cramDigest;
        try {
            cramDigest = _getCramDigest(cramSecret, challenge);
        } catch (NoSuchAlgorithmException e) {
            throw new AtException("Failed to generate cramDigest", e);
        }

        String cramResponse = connection.executeCommand("cram:" + cramDigest);
        if (! cramResponse.startsWith("data:success")) {
            throw new AtException("CRAM command failed: " + cramResponse);
        }
    }

    public void authenticateWithPkam(AtConnection connection, AtSign atSign, Map<String, String> keys) throws AtException, IOException {
        if (! keys.containsKey(KeysUtil.pkamPrivateKeyName)) {
            throw new AtException("Cannot authenticate with PKAM: Keys file does not contain " + KeysUtil.pkamPrivateKeyName);
        }

        String fromResponse = connection.executeCommand("from:" + atSign);

        String dataPrefix = "data:";
        if (! fromResponse.startsWith(dataPrefix)) {
            throw new AtException("from command got invalid from response: " + fromResponse);
        }
        fromResponse = fromResponse.substring(dataPrefix.length());

        PrivateKey privateKey;
        try {
            privateKey = EncryptionUtil.privateKeyFromBase64(keys.get(KeysUtil.pkamPrivateKeyName));
        } catch (Exception e) {
            throw new AtException("Failed to get private key from stored string: " + e.getMessage());
        }

        String signature;
        try {
            signature = EncryptionUtil.signSHA256RSA(fromResponse, privateKey);
        } catch (Exception e) {
            throw new AtException("Failed to create SHA256 signature: " + e.getMessage());
        }

        String pkamResponse = connection.executeCommand("pkam:" + signature);

        if (! pkamResponse.startsWith("data:success")) {
            throw new AtException("PKAM command failed: " + pkamResponse);
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
