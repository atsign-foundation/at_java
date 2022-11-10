package org.atsign.client.util;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.atsign.common.AtException;
import org.atsign.common.AtSign;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

public class KeysUtil {
    private static final ObjectMapper mapper = new ObjectMapper();

    private static String rootFolder = System.getProperty("user.home") + "/.atsign/keys/";

    public static final String pkamPublicKeyName = "aesPkamPublicKey";
    public static final String pkamPrivateKeyName = "aesPkamPrivateKey";
    public static final String encryptionPublicKeyName = "aesEncryptPublicKey";
    public static final String encryptionPrivateKeyName = "aesEncryptPrivateKey";
    public static final String selfEncryptionKeyName = "selfEncryptionKey";

    public static void saveKeys(AtSign atSign, Map<String, String> keys) throws Exception {
        rootFolder += atSign + "_key.atKeys";
        File file = new File(rootFolder);
        // if keys do not exist in project ~/, search in dir
        if (!file.exists()) {
            rootFolder = System.getProperty("user.dir") + "/keys/" + atSign + "_key.atKeys";
            // _makeRootFolder();
            file = new File(rootFolder);
            if (!file.exists()) {
                // If atKeys file does not exist,
                // create dir in ~/ and request
                // to store key files there
                _makeRootFolder();
            }
            System.out.println("Saving keys to " + file.getAbsolutePath());
        } else {
            System.out.println("Saving keys to " + file.getAbsolutePath());
            // noinspection ResultOfMethodCallIgnored
            file.delete();
        }

        String selfEncryptionKey = keys.get(selfEncryptionKeyName);

        Map<String, String> encryptedKeys = new TreeMap<>();

        // We encrypt all the keys with the AES self encryption key (which is left
        // unencrypted)
        encryptedKeys.put(selfEncryptionKeyName, selfEncryptionKey);
        encryptedKeys.put(pkamPublicKeyName,
                EncryptionUtil.aesEncryptToBase64(keys.get(pkamPublicKeyName), selfEncryptionKey));
        encryptedKeys.put(pkamPrivateKeyName,
                EncryptionUtil.aesEncryptToBase64(keys.get(pkamPrivateKeyName), selfEncryptionKey));
        encryptedKeys.put(encryptionPublicKeyName,
                EncryptionUtil.aesEncryptToBase64(keys.get(encryptionPublicKeyName), selfEncryptionKey));
        encryptedKeys.put(encryptionPrivateKeyName,
                EncryptionUtil.aesEncryptToBase64(keys.get(encryptionPrivateKeyName), selfEncryptionKey));

        String json = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(encryptedKeys);
        Files.write(file.toPath(), json.getBytes(StandardCharsets.UTF_8));
    }

    public static Map<String, String> loadKeys(AtSign atSign) throws Exception {
        File file = new File(rootFolder);
        if (!file.exists()) {
            // if keys do not exist in project dir, search in ~/
            rootFolder = System.getProperty("user.home") + "/.atsign/keys/" + atSign + "_key.atKeys";
            file = new File(rootFolder);
            if (!file.exists()) {
                _makeRootFolder();
            }
        }
        String json = new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8);
        @SuppressWarnings("unchecked")
        Map<String, String> encryptedKeys = mapper.readValue(json, Map.class);

        // All the keys are encrypted with the AES self encryption key (which is left
        // unencrypted)
        String selfEncryptionKey = encryptedKeys.get(selfEncryptionKeyName);

        Map<String, String> keys = new HashMap<>();
        keys.put(selfEncryptionKeyName, selfEncryptionKey);
        keys.put(pkamPublicKeyName,
                EncryptionUtil.aesDecryptFromBase64(encryptedKeys.get(pkamPublicKeyName), selfEncryptionKey));
        keys.put(pkamPrivateKeyName,
                EncryptionUtil.aesDecryptFromBase64(encryptedKeys.get(pkamPrivateKeyName), selfEncryptionKey));
        keys.put(encryptionPublicKeyName,
                EncryptionUtil.aesDecryptFromBase64(encryptedKeys.get(encryptionPublicKeyName), selfEncryptionKey));
        keys.put(encryptionPrivateKeyName,
                EncryptionUtil.aesDecryptFromBase64(encryptedKeys.get(encryptionPrivateKeyName), selfEncryptionKey));
        return keys;
    }

    // Changing _makeRootFolder to be a last resort func
    // This creates a dir in ~/
    private static void _makeRootFolder() throws IOException, AtException {
        rootFolder = System.getProperty("user.home") + "/.atsign/keys/";
        Path rootDir = Paths.get(rootFolder);
        Files.createDirectories(rootDir);
        throw new AtException("loadKeys: No file at ~/.atsign/keys or your_proj/keys" +
                "\t Please store atsign keys within dir ~/.atsign/keys/");
    }
}
