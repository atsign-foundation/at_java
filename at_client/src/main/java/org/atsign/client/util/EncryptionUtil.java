package org.atsign.client.util;

import org.bouncycastle.jce.provider.BouncyCastleProvider;

import javax.crypto.*;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.security.spec.EncodedKeySpec;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

public class EncryptionUtil {
    static final IvParameterSpec IV = new IvParameterSpec(new byte[16]);

    static {
        Security.addProvider(new BouncyCastleProvider());
    }

    public static String aesEncryptToBase64(String clearText, String keyBase64) throws NoSuchPaddingException, NoSuchAlgorithmException, InvalidAlgorithmParameterException, InvalidKeyException, IllegalBlockSizeException, BadPaddingException, NoSuchProviderException {
        SecretKey key = _aesKeyFromBase64(keyBase64);
        Cipher cipher = Cipher.getInstance("AES/SIC/PKCS7Padding", "BC");
        cipher.init(Cipher.ENCRYPT_MODE, key, IV);
        byte[] encrypted = cipher.doFinal(clearText.getBytes());
        return Base64.getEncoder().encodeToString(encrypted);
    }

    public static String aesDecryptFromBase64(String cipherTextBase64, String keyBase64) throws NoSuchPaddingException, NoSuchAlgorithmException, InvalidAlgorithmParameterException, InvalidKeyException, IllegalBlockSizeException, BadPaddingException, NoSuchProviderException {
        SecretKey key = _aesKeyFromBase64(keyBase64);
        Cipher cipher = Cipher.getInstance("AES/SIC/PKCS7Padding", "BC");
        cipher.init(Cipher.DECRYPT_MODE, key, IV);
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

    public static String rsaDecryptFromBase64(String cipherTextBase64, String privateKeyBase64) throws NoSuchAlgorithmException, InvalidKeySpecException, NoSuchPaddingException, InvalidKeyException, IllegalBlockSizeException, BadPaddingException {
        PrivateKey privateKey = _privateKeyFromBase64(privateKeyBase64);
        Cipher decryptCipher = Cipher.getInstance("RSA");
        decryptCipher.init(Cipher.DECRYPT_MODE, privateKey);
        byte[] decoded = Base64.getDecoder().decode(cipherTextBase64.getBytes(StandardCharsets.UTF_8));
        byte[] decryptedMessageBytes = decryptCipher.doFinal(decoded);

        return new String(decryptedMessageBytes, StandardCharsets.UTF_8);
    }

    public static String rsaEncryptToBase64(String clearText, String publicKeyBase64) throws NoSuchAlgorithmException, InvalidKeySpecException, NoSuchPaddingException, InvalidKeyException, IllegalBlockSizeException, BadPaddingException {
        PublicKey publicKey = _publicKeyFromBase64(publicKeyBase64);
        Cipher encryptCipher = Cipher.getInstance("RSA");
        encryptCipher.init(Cipher.ENCRYPT_MODE, publicKey);
        byte[] clearTextBytes = clearText.getBytes(StandardCharsets.UTF_8);
        byte[] encryptedMessageBytes = encryptCipher.doFinal(clearTextBytes);

        return Base64.getEncoder().encodeToString(encryptedMessageBytes);
    }

    public static String signSHA256RSA(String value, String privateKeyBase64) throws NoSuchAlgorithmException, InvalidKeySpecException, SignatureException, InvalidKeyException {
        PrivateKey privateKey = _privateKeyFromBase64(privateKeyBase64);
        String signed = _signSHA256RSA(value, privateKey);
        return signed;
    }

    // non-public methods
    static String _signSHA256RSA(String input, PrivateKey pk) throws NoSuchAlgorithmException, InvalidKeyException, SignatureException {
        Signature privateSignature = Signature.getInstance("SHA256withRSA");
        privateSignature.initSign(pk);
        privateSignature.update(input.getBytes(StandardCharsets.UTF_8));
        byte[] signedBytes = privateSignature.sign();
        return Base64.getEncoder().encodeToString(signedBytes);
    }

    static PublicKey _publicKeyFromBase64 (String s) throws NoSuchAlgorithmException, InvalidKeySpecException {
        byte[] keyBytes = Base64.getDecoder().decode(s.getBytes(StandardCharsets.UTF_8));
        EncodedKeySpec keySpec = new X509EncodedKeySpec(keyBytes);
        KeyFactory rsaKeyFactory = KeyFactory.getInstance("RSA");
        return rsaKeyFactory.generatePublic(keySpec);
    }

    static PrivateKey _privateKeyFromBase64(String s) throws NoSuchAlgorithmException, InvalidKeySpecException {
        byte[] keyBytes = Base64.getDecoder().decode(s.getBytes(StandardCharsets.UTF_8));
        PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(keyBytes);
        KeyFactory rsaKeyFactory = KeyFactory.getInstance("RSA");
        return rsaKeyFactory.generatePrivate(keySpec);
    }

    static SecretKey _aesKeyFromBase64(String s) {
        byte[] keyBytes = Base64.getDecoder().decode(s.getBytes());
        return new SecretKeySpec(keyBytes, "AES");
    }
}
