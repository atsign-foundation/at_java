package org.atsign.client.impl.util;

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

import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * Utility class which registers bouncycastle as a security {@link Provider} and provides
 * static methods for the various encryption functions required by Atsign client APIs
 */
public class EncryptionUtils {

  /**
   * The signing algo "label"
   */
  public static final String SIGNING_ALGO_RSA = "rsa2048";

  /**
   * The hashing algo "label"
   */
  public static final String HASHING_ALGO_SHA256 = "sha256";

  static {
    Security.addProvider(new BouncyCastleProvider());
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
      byte[] encrypted = cipher.doFinal(input.getBytes(UTF_8));
      return Base64.getEncoder().encodeToString(encrypted);
    } catch (NoSuchAlgorithmException | NoSuchProviderException | BadPaddingException | IllegalBlockSizeException
        | NoSuchPaddingException | InvalidKeyException | InvalidAlgorithmParameterException e) {
      throw new AtEncryptionException("AES encryption failed", e);
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
      byte[] decrypted = cipher.doFinal(Base64.getDecoder().decode(input));
      return new String(decrypted, UTF_8);
    } catch (NoSuchAlgorithmException | NoSuchProviderException | NoSuchPaddingException | InvalidKeyException
        | InvalidAlgorithmParameterException | IllegalBlockSizeException | BadPaddingException e) {
      throw new AtDecryptionException("AES decryption failed", e);
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
      byte[] decoded = Base64.getDecoder().decode(input.getBytes(UTF_8));
      byte[] decryptedMessageBytes = decryptCipher.doFinal(decoded);
      return new String(decryptedMessageBytes, UTF_8);
    } catch (NoSuchAlgorithmException | InvalidKeySpecException | NoSuchPaddingException | InvalidKeyException
        | IllegalBlockSizeException | BadPaddingException e) {
      throw new AtDecryptionException("RSA decryption failed", e);
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
      byte[] clearTextBytes = input.getBytes(UTF_8);
      byte[] encryptedMessageBytes = encryptCipher.doFinal(clearTextBytes);
      return Base64.getEncoder().encodeToString(encryptedMessageBytes);
    } catch (NoSuchAlgorithmException | InvalidKeySpecException | NoSuchPaddingException | InvalidKeyException
        | IllegalBlockSizeException | BadPaddingException e) {
      throw new AtEncryptionException("RSA encryption failed", e);
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
      privateSignature.update(input.getBytes(UTF_8));
      byte[] signedBytes = privateSignature.sign();
      return Base64.getEncoder().encodeToString(signedBytes);
    } catch (NoSuchAlgorithmException | InvalidKeySpecException | InvalidKeyException | SignatureException e) {
      throw new AtEncryptionException("SHA256 sign failed", e);
    }
  }

  private static PublicKey toPublicKey(String s) throws NoSuchAlgorithmException, InvalidKeySpecException {
    byte[] keyBytes = Base64.getDecoder().decode(s.getBytes(UTF_8));
    EncodedKeySpec keySpec = new X509EncodedKeySpec(keyBytes);
    KeyFactory rsaKeyFactory = KeyFactory.getInstance("RSA");
    return rsaKeyFactory.generatePublic(keySpec);
  }

  private static PrivateKey toPrivateKey(String s) throws NoSuchAlgorithmException, InvalidKeySpecException {
    byte[] keyBytes = Base64.getDecoder().decode(s.getBytes(UTF_8));
    PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(keyBytes);
    KeyFactory rsaKeyFactory = KeyFactory.getInstance("RSA");
    return rsaKeyFactory.generatePrivate(keySpec);
  }

  private static SecretKey toSecretKey(String s) {
    byte[] keyBytes = Base64.getDecoder().decode(s.getBytes());
    return new SecretKeySpec(keyBytes, "AES");
  }

  private static IvParameterSpec toIvParameterSpec(String s) {
    byte[] ivBytes = Base64.getDecoder().decode(s.getBytes());
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

  private static Cipher createAesCipher(int mode, String keyBase64, String ivNonce) throws NoSuchAlgorithmException,
      NoSuchProviderException, NoSuchPaddingException, InvalidKeyException, InvalidAlgorithmParameterException {
    SecretKey key = toSecretKey(keyBase64);
    IvParameterSpec iv = toIvParameterSpec(ivNonce);
    Cipher cipher = Cipher.getInstance("AES/SIC/PKCS7Padding", "BC");
    cipher.init(mode, key, iv);
    return cipher;
  }

}
