package org.atsign.client.util;

import org.atsign.common.AtSign;
import org.atsign.client.api.impl.connections.AtSecondaryConnection;

import java.io.IOException;
import java.security.*;
import java.util.Base64;
import java.util.Map;

public class OnboardingUtil {
    public void generatePkamKeypair(Map<String, String> keys) throws NoSuchAlgorithmException {
        // 	generate pkam keypair; store to files
        KeyPair keyPair = EncryptionUtil.generateRSAKeyPair();
        PublicKey publicKey = keyPair.getPublic();
        PrivateKey privateKey = keyPair.getPrivate();

        String publicKeyString = Base64.getEncoder().encodeToString(publicKey.getEncoded());
        String privateKeyString = Base64.getEncoder().encodeToString(privateKey.getEncoded());

        keys.put(KeysUtil.pkamPublicKeyName, publicKeyString);
        keys.put(KeysUtil.pkamPrivateKeyName, privateKeyString);
    }

    public void generateEncryptionKeypair(Map<String, String> keys) throws NoSuchAlgorithmException {
        // 	generate pkam keypair; store to files
        KeyPair keyPair = EncryptionUtil.generateRSAKeyPair();
        PublicKey publicKey = keyPair.getPublic();
        PrivateKey privateKey = keyPair.getPrivate();

        String publicKeyString = Base64.getEncoder().encodeToString(publicKey.getEncoded());
        String privateKeyString = Base64.getEncoder().encodeToString(privateKey.getEncoded());

        keys.put(KeysUtil.encryptionPublicKeyName, publicKeyString);
        keys.put(KeysUtil.encryptionPrivateKeyName, privateKeyString);
    }


    public void generateSelfEncryptionKey(Map<String, String> keys) throws NoSuchAlgorithmException {
        String selfEncryptionKey = EncryptionUtil.generateAESKeyBase64();
        keys.put(KeysUtil.selfEncryptionKeyName, selfEncryptionKey);
    }

    public void storePkamPublicKey(AtSecondaryConnection connection, Map<String, String> keys) throws IOException {
        //	send update:privatekey:at_pkam_publickey $pkamKeyPair.publicKey
        connection.executeCommand("update:privatekey:at_pkam_publickey " + keys.get(KeysUtil.pkamPublicKeyName));
    }

    public void storePublicEncryptionKey(AtSecondaryConnection connection, AtSign atSign, Map<String, String> keys) throws IOException {
        //	send update:public:publickey@atSign encryptionPublicKey
        connection.executeCommand("update:public:publickey" + atSign.toString() + " " + keys.get(KeysUtil.encryptionPublicKeyName));
    }

    public void deleteCramKey(AtSecondaryConnection connection) throws IOException {
        //	send delete:privatekey:at_secret
        connection.executeCommand("delete:privatekey:at_secret");
    }
}
