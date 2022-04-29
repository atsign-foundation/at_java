package org.atsign.client.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.atsign.common.AtSign;
import org.atsign.common.AtException;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

public class KeysUtil {
    private static final ObjectMapper mapper = new ObjectMapper();

    private static final String rootFolder = System.getProperty("user.dir") + "/keys/";
    public static String pkamPublicKeyName = "aesPkamPublicKey";
    public static String pkamPrivateKeyName = "aesPkamPrivateKey";
    public static String encryptionPublicKeyName = "aesEncryptPublicKey";
    public static String encryptionPrivateKeyName = "aesEncryptPrivateKey";
    public static String selfEncryptionKeyName = "selfEncryptionKey";

    public static void saveKeys(AtSign atSign, Map<String, String> keys) throws Exception {
        _makeRootFolder();
        File file = new File(rootFolder + atSign + ".keys");
        System.out.println("Saving keys to " + file.getAbsolutePath());
        if (file.exists()) {
            //noinspection ResultOfMethodCallIgnored
            file.delete();
        }

        String selfEncryptionKey = keys.get(selfEncryptionKeyName);

        Map<String, String> encryptedKeys = new TreeMap<>();

        // We encrypt all the keys with the AES self encryption key (which is left unencrypted)
        encryptedKeys.put(selfEncryptionKeyName, selfEncryptionKey);
        encryptedKeys.put(pkamPublicKeyName, EncryptionUtil.aesEncryptToBase64(keys.get(pkamPublicKeyName), selfEncryptionKey));
        encryptedKeys.put(pkamPrivateKeyName, EncryptionUtil.aesEncryptToBase64(keys.get(pkamPrivateKeyName), selfEncryptionKey));
        encryptedKeys.put(encryptionPublicKeyName, EncryptionUtil.aesEncryptToBase64(keys.get(encryptionPublicKeyName), selfEncryptionKey));
        encryptedKeys.put(encryptionPrivateKeyName, EncryptionUtil.aesEncryptToBase64(keys.get(encryptionPrivateKeyName), selfEncryptionKey));

        String json = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(encryptedKeys);
        Files.write(file.toPath(), json.getBytes(StandardCharsets.UTF_8));
    }

    public static Map<String, String> loadKeys(AtSign atSign) throws Exception {
        _makeRootFolder();
        File file = new File(rootFolder + atSign + ".keys");
        if (! file.exists()) {
            throw new AtException("loadKeys: No file at " + file.getAbsolutePath());
        }

        String json = new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8);
        @SuppressWarnings("unchecked") Map<String, String> encryptedKeys = mapper.readValue(json, Map.class);

        // All the keys are encrypted with the AES self encryption key (which is left unencrypted)
        String selfEncryptionKey = encryptedKeys.get(selfEncryptionKeyName);

        Map<String, String> keys = new HashMap<>();
        keys.put(selfEncryptionKeyName, selfEncryptionKey);
        keys.put(pkamPublicKeyName, EncryptionUtil.aesDecryptFromBase64(encryptedKeys.get(pkamPublicKeyName), selfEncryptionKey));
        keys.put(pkamPrivateKeyName, EncryptionUtil.aesDecryptFromBase64(encryptedKeys.get(pkamPrivateKeyName), selfEncryptionKey));
        keys.put(encryptionPublicKeyName, EncryptionUtil.aesDecryptFromBase64(encryptedKeys.get(encryptionPublicKeyName), selfEncryptionKey));
        keys.put(encryptionPrivateKeyName, EncryptionUtil.aesDecryptFromBase64(encryptedKeys.get(encryptionPrivateKeyName), selfEncryptionKey));
        return keys;
    }

    private static void _makeRootFolder() throws IOException {
        Files.createDirectories(Paths.get(rootFolder));
    }
}
