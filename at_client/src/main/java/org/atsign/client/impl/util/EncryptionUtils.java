package org.atsign.client.impl.util;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.atsign.client.impl.common.Preconditions.checkNotBlank;

import java.security.*;
import java.security.spec.EncodedKeySpec;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

import javax.crypto.*;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import org.atsign.client.impl.exceptions.AtDecryptionException;
import org.atsign.client.impl.exceptions.AtEncryptionException;
import org.bouncycastle.jce.provider.BouncyCastleProvider;

/**
 * Utility class which registers bouncycastle as a security {@link Provider} and provides
 * static methods for the various encryption functions required by Atsign client APIs
 */
public class EncryptionUtils {

  private static final char[] HEX_ARRAY = "0123456789abcdef".toCharArray();

  /**
   * The signing algo "label"
   */
  public static final String SIGNING_ALGO_RSA = "rsa2048";

  /**
   * The hashing algo "label"
   */
  public static final String HASHING_ALGO_SHA256 = "sha256";

  /**
   * The hashing algo "label"
   */
  public static final String HASHING_ALGO_SHA512 = "sha512";

  /**
   * The checksum algo
   */
  public static final String MD5 = "MD5";

  static {
    Security.addProvider(new BouncyCastleProvider());
  }

  /**
   * Creates a digest for the given input using the given algo.
   *
   * @param input The text to digest.
   * @param algo The digest algorithm to use. e.g. MD5
   * @return The digest as a hex string.
   * @throws AtEncryptionException If algorithm cannot be found or any other error.
   */
  public static String digest(String input, String algo) throws AtEncryptionException {
    try {
      MessageDigest md = MessageDigest.getInstance(toMessageDigestAlgorithm(algo));
      return bytesToHex(md.digest(checkNotBlank(input, "input blank").getBytes(UTF_8)));
    } catch (IllegalArgumentException | NoSuchAlgorithmException e) {
      throw new AtEncryptionException("failed to hash : " + e.getMessage(), e);
    }
  }

  private static String toMessageDigestAlgorithm(String algo) {
    checkNotBlank(algo, "algo blank");
    switch (algo) {
      case HASHING_ALGO_SHA256:
        return "SHA-256";
      case HASHING_ALGO_SHA512:
        return "SHA-512";
      default:
        return algo;
    }
  }

  /**
   * Encrypts a String with the AES Cipher and encodes as Base 64.
   *
   * @param input The String to encrypt
   * @param key The AES symmetric key to use.
   * @param iv An initialization vector to use.
   * @return The Base64 encoded encrypted String.
   * @throws AtEncryptionException If something fails.
   */
  public static String aesEncryptToBase64(String input, String key, String iv)
      throws AtEncryptionException {
    try {
      Cipher cipher = createAesCipher(Cipher.ENCRYPT_MODE, key, iv);
      byte[] encrypted = cipher.doFinal(checkNotBlank(input, "input is blank").getBytes(UTF_8));
      return Base64.getEncoder().encodeToString(encrypted);
    } catch (IllegalArgumentException | NoSuchAlgorithmException | NoSuchProviderException | BadPaddingException
        | IllegalBlockSizeException | NoSuchPaddingException | InvalidKeyException
        | InvalidAlgorithmParameterException e) {
      throw new AtEncryptionException("AES encryption failed : " + e.getMessage(), e);
    }
  }

  /**
   * Decrypts a Base64 encoded String with the AES Cipher.
   *
   * @param input The Base64 encoded String to decrypt.
   * @param key The AES symmetric key to use.
   * @param iv An initialization vector that was used.
   * @return The decrypted String.
   * @throws AtDecryptionException If something fails.
   */
  public static String aesDecryptFromBase64(String input, String key, String iv) throws AtDecryptionException {
    try {
      Cipher cipher = createAesCipher(Cipher.DECRYPT_MODE, key, iv);
      byte[] decrypted = cipher.doFinal(Base64.getDecoder().decode(checkNotBlank(input, "input is blank")));
      return new String(decrypted, UTF_8);
    } catch (IllegalArgumentException | NoSuchAlgorithmException | NoSuchProviderException | NoSuchPaddingException
        | InvalidKeyException | InvalidAlgorithmParameterException | IllegalBlockSizeException
        | BadPaddingException e) {
      throw new AtDecryptionException("AES decryption failed : " + e.getMessage(), e);
    }
  }

  /**
   * Generate a new RSA Key Pair.
   *
   * @return A new RSA {@link KeyPair}.
   * @throws AtEncryptionException If something fails.
   */
  public static KeyPair generateRSAKeyPair() throws AtEncryptionException {
    try {
      KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
      generator.initialize(2048);
      return generator.generateKeyPair();
    } catch (NoSuchAlgorithmException e) {
      throw new AtEncryptionException(e.getMessage());
    }
  }

  /**
   * Generate a new AES symmetric key.
   *
   * @return A new Base 64 encoded AES symmetric key.
   * @throws AtEncryptionException If something fails.
   */
  public static String generateAESKeyBase64() throws AtEncryptionException {
    try {
      KeyGenerator keyGenerator = KeyGenerator.getInstance("AES");
      keyGenerator.init(256);
      byte[] key = keyGenerator.generateKey().getEncoded();
      return Base64.getEncoder().encodeToString(key);
    } catch (NoSuchAlgorithmException e) {
      throw new AtEncryptionException(e.getMessage());
    }
  }

  /**
   * Decrypts a Base64 encoded String with the RSA Cipher.
   *
   * @param input The Base64 encoded String to decrypt.
   * @param key The private RSA key to use.
   * @return The decrypted String.
   * @throws AtDecryptionException If something fails.
   */
  public static String rsaDecryptFromBase64(String input, String key) throws AtDecryptionException {
    try {
      PrivateKey privateKey = toPrivateKey(key);
      Cipher decryptCipher = Cipher.getInstance("RSA");
      decryptCipher.init(Cipher.DECRYPT_MODE, privateKey);
      byte[] decoded = Base64.getDecoder().decode(checkNotBlank(input, "input is blank").getBytes(UTF_8));
      byte[] decryptedMessageBytes = decryptCipher.doFinal(decoded);
      return new String(decryptedMessageBytes, UTF_8);
    } catch (IllegalArgumentException | NoSuchAlgorithmException | InvalidKeySpecException | NoSuchPaddingException
        | InvalidKeyException | IllegalBlockSizeException | BadPaddingException e) {
      throw new AtDecryptionException("RSA decryption failed : " + e.getMessage(), e);
    }
  }

  /**
   * Encrypts a String with the RSA Cipher and encodes as Base 64.
   *
   * @param input The String to encrypt
   * @param key The RSA public key to use.
   * @return The Base64 encoded encrypted String.
   * @throws AtEncryptionException If something fails.
   */
  public static String rsaEncryptToBase64(String input, String key) throws AtEncryptionException {
    try {
      PublicKey publicKey = toPublicKey(key);
      Cipher encryptCipher = Cipher.getInstance("RSA");
      encryptCipher.init(Cipher.ENCRYPT_MODE, publicKey);
      byte[] clearTextBytes = checkNotBlank(input, "input is blank").getBytes(UTF_8);
      byte[] encryptedMessageBytes = encryptCipher.doFinal(clearTextBytes);
      return Base64.getEncoder().encodeToString(encryptedMessageBytes);
    } catch (IllegalArgumentException | NoSuchAlgorithmException | InvalidKeySpecException | NoSuchPaddingException
        | InvalidKeyException | IllegalBlockSizeException | BadPaddingException e) {
      throw new AtEncryptionException("RSA encryption failed : " + e.getMessage(), e);
    }
  }

  /**
   * Creates signature and encodes as Base 64.
   *
   * @param input The String to encrypt
   * @param key The RSA private key to use.
   * @return The Base64 encoded signature.
   * @throws AtEncryptionException If something fails.
   */
  public static String signSHA256RSA(String input, String key) throws AtEncryptionException {
    try {
      PrivateKey pk = toPrivateKey(key);
      Signature privateSignature = Signature.getInstance("SHA256withRSA");
      privateSignature.initSign(pk);
      privateSignature.update(checkNotBlank(input, "input is blank").getBytes(UTF_8));
      byte[] signedBytes = privateSignature.sign();
      return Base64.getEncoder().encodeToString(signedBytes);
    } catch (IllegalArgumentException | NoSuchAlgorithmException | InvalidKeySpecException | InvalidKeyException
        | SignatureException e) {
      throw new AtEncryptionException("SHA256 sign failed : " + e.getMessage(), e);
    }
  }

  private static PublicKey toPublicKey(String s) throws NoSuchAlgorithmException, InvalidKeySpecException {
    byte[] keyBytes = Base64.getDecoder().decode(checkNotBlank(s, "key is blank").getBytes(UTF_8));
    EncodedKeySpec keySpec = new X509EncodedKeySpec(keyBytes);
    KeyFactory rsaKeyFactory = KeyFactory.getInstance("RSA");
    return rsaKeyFactory.generatePublic(keySpec);
  }

  private static PrivateKey toPrivateKey(String s) throws NoSuchAlgorithmException, InvalidKeySpecException {
    byte[] keyBytes = Base64.getDecoder().decode(checkNotBlank(s, "key is blank").getBytes(UTF_8));
    PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(keyBytes);
    KeyFactory rsaKeyFactory = KeyFactory.getInstance("RSA");
    return rsaKeyFactory.generatePrivate(keySpec);
  }

  private static SecretKey toSecretKey(String s) {
    byte[] keyBytes = Base64.getDecoder().decode(checkNotBlank(s, "key is blank").getBytes());
    return new SecretKeySpec(keyBytes, "AES");
  }

  private static IvParameterSpec toIvParameterSpec(String s) {
    byte[] ivBytes = Base64.getDecoder().decode(checkNotBlank(s, "iv is blank").getBytes());
    return new IvParameterSpec(ivBytes);
  }

  /**
   * Creates random initialization vector.
   *
   * @param length of vector required.
   * @return The Base 64 encoded vector.
   */
  public static String generateRandomIvBase64(int length) {
    byte[] iv = new byte[length];
    SecureRandom secureRandom = new SecureRandom();
    secureRandom.nextBytes(iv);
    return Base64.getEncoder().encodeToString(iv);
  }

  public static String bytesToHex(byte[] bytes) {
    char[] hexChars = new char[bytes.length * 2];
    for (int j = 0; j < bytes.length; j++) {
      int v = bytes[j] & 0xFF;
      hexChars[j * 2] = HEX_ARRAY[v >>> 4];
      hexChars[j * 2 + 1] = HEX_ARRAY[v & 0x0F];
    }
    return new String(hexChars);
  }

  public static String toStringBase64(Key key) {
    return Base64.getEncoder().encodeToString(key.getEncoded());
  }

  private static Cipher createAesCipher(int mode, String keyBase64, String ivNonce) throws NoSuchAlgorithmException,
      NoSuchProviderException, NoSuchPaddingException, InvalidKeyException, InvalidAlgorithmParameterException {
    SecretKey key = toSecretKey(keyBase64);
    IvParameterSpec iv = toIvParameterSpec(ivNonce);
    Cipher cipher = Cipher.getInstance("AES/SIC/PKCS7Padding", "BC");
    cipher.init(mode, key, iv);
    return cipher;
  }

}
