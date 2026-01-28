package org.atsign.client.util;

import java.nio.charset.StandardCharsets;
import java.security.*;
import java.security.spec.EncodedKeySpec;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

import javax.crypto.*;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import org.bouncycastle.jce.provider.BouncyCastleProvider;

public class EncryptionUtil {

  public static final String SIGNING_ALGO_RSA = "rsa2048";
  public static final String HASHING_ALGO_SHA256 = "sha256";

  static {
    Security.addProvider(new BouncyCastleProvider());
  }

  public static String aesEncryptToBase64(String clearText, String keyBase64, String ivNonce)
      throws NoSuchPaddingException, NoSuchAlgorithmException, InvalidAlgorithmParameterException, InvalidKeyException,
      IllegalBlockSizeException, BadPaddingException, NoSuchProviderException {
    Cipher cipher = createAesCipher(Cipher.ENCRYPT_MODE, keyBase64, ivNonce);
    byte[] encrypted = cipher.doFinal(clearText.getBytes());
    return Base64.getEncoder().encodeToString(encrypted);
  }

  public static String aesDecryptFromBase64(String cipherTextBase64, String keyBase64, String ivNonce)
      throws NoSuchPaddingException, NoSuchAlgorithmException, InvalidAlgorithmParameterException, InvalidKeyException,
      IllegalBlockSizeException, BadPaddingException, NoSuchProviderException {
    Cipher cipher = createAesCipher(Cipher.DECRYPT_MODE, keyBase64, ivNonce);
    byte[] decrypted = cipher.doFinal(Base64.getDecoder().decode(cipherTextBase64));
    return new String(decrypted);
  }

  public static KeyPair generateRSAKeyPair() throws NoSuchAlgorithmException {
    KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
    generator.initialize(2048);
    return generator.generateKeyPair();
  }

  public static String generateAESKeyBase64() throws NoSuchAlgorithmException {
    KeyGenerator keyGenerator = KeyGenerator.getInstance("AES");
    keyGenerator.init(256);
    byte[] key = keyGenerator.generateKey().getEncoded();
    return Base64.getEncoder().encodeToString(key);
  }

  public static String rsaDecryptFromBase64(String cipherTextBase64, String privateKeyBase64)
      throws NoSuchAlgorithmException, InvalidKeySpecException, NoSuchPaddingException, InvalidKeyException,
      IllegalBlockSizeException, BadPaddingException {
    PrivateKey privateKey = _privateKeyFromBase64(privateKeyBase64);
    Cipher decryptCipher = Cipher.getInstance("RSA");
    decryptCipher.init(Cipher.DECRYPT_MODE, privateKey);
    byte[] decoded = Base64.getDecoder().decode(cipherTextBase64.getBytes(StandardCharsets.UTF_8));
    byte[] decryptedMessageBytes = decryptCipher.doFinal(decoded);

    return new String(decryptedMessageBytes, StandardCharsets.UTF_8);
  }

  public static String rsaEncryptToBase64(String clearText, String publicKeyBase64)
      throws NoSuchAlgorithmException, InvalidKeySpecException, NoSuchPaddingException, InvalidKeyException,
      IllegalBlockSizeException, BadPaddingException {
    PublicKey publicKey = _publicKeyFromBase64(publicKeyBase64);
    Cipher encryptCipher = Cipher.getInstance("RSA");
    encryptCipher.init(Cipher.ENCRYPT_MODE, publicKey);
    byte[] clearTextBytes = clearText.getBytes(StandardCharsets.UTF_8);
    byte[] encryptedMessageBytes = encryptCipher.doFinal(clearTextBytes);

    return Base64.getEncoder().encodeToString(encryptedMessageBytes);
  }

  public static String signSHA256RSA(String value, String privateKeyBase64)
      throws NoSuchAlgorithmException, InvalidKeySpecException, SignatureException, InvalidKeyException {
    PrivateKey privateKey = _privateKeyFromBase64(privateKeyBase64);
    return _signSHA256RSA(value, privateKey);
  }

  // non-public methods
  public static String _signSHA256RSA(String input, PrivateKey pk)
      throws NoSuchAlgorithmException, InvalidKeyException, SignatureException {
    Signature privateSignature = Signature.getInstance("SHA256withRSA");
    privateSignature.initSign(pk);
    privateSignature.update(input.getBytes(StandardCharsets.UTF_8));
    byte[] signedBytes = privateSignature.sign();
    return Base64.getEncoder().encodeToString(signedBytes);
  }

  public static PublicKey _publicKeyFromBase64(String s) throws NoSuchAlgorithmException, InvalidKeySpecException {
    byte[] keyBytes = Base64.getDecoder().decode(s.getBytes(StandardCharsets.UTF_8));
    EncodedKeySpec keySpec = new X509EncodedKeySpec(keyBytes);
    KeyFactory rsaKeyFactory = KeyFactory.getInstance("RSA");
    return rsaKeyFactory.generatePublic(keySpec);
  }

  public static PrivateKey _privateKeyFromBase64(String s) throws NoSuchAlgorithmException, InvalidKeySpecException {
    byte[] keyBytes = Base64.getDecoder().decode(s.getBytes(StandardCharsets.UTF_8));
    PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(keyBytes);
    KeyFactory rsaKeyFactory = KeyFactory.getInstance("RSA");
    return rsaKeyFactory.generatePrivate(keySpec);
  }

  public static SecretKey _aesKeyFromBase64(String s) {
    byte[] keyBytes = Base64.getDecoder().decode(s.getBytes());
    return new SecretKeySpec(keyBytes, "AES");
  }

  public static IvParameterSpec _ivFromBase64(String s) {
    byte[] ivBytes = Base64.getDecoder().decode(s.getBytes());
    return new IvParameterSpec(ivBytes);
  }

  public static String generateRandomIvBase64(int length) {
    byte[] iv = new byte[length];
    SecureRandom secureRandom = new SecureRandom();
    secureRandom.nextBytes(iv);
    return Base64.getEncoder().encodeToString(iv);
  }

  private static Cipher createAesCipher(int mode, String keyBase64, String ivNonce) throws NoSuchAlgorithmException,
      NoSuchProviderException, NoSuchPaddingException, InvalidKeyException, InvalidAlgorithmParameterException {
    SecretKey key = _aesKeyFromBase64(keyBase64);
    IvParameterSpec iv = _ivFromBase64(ivNonce);
    Cipher cipher = Cipher.getInstance("AES/SIC/PKCS7Padding", "BC");
    cipher.init(mode, key, iv);
    return cipher;
  }

}
