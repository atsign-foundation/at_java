package org.atsign.client.util;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.atsign.common.AtException;
import org.atsign.common.AtSign;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

public class KeysUtil {
    private static final ObjectMapper mapper = new ObjectMapper();

    private static final String rootFolder = System.getProperty("user.dir") + "/keys/";

    public  static final String pkamPublicKeyName = "aesPkamPublicKey";
    public  static final String pkamPrivateKeyName = "aesPkamPrivateKey";
    public  static final String encryptionPublicKeyName = "aesEncryptPublicKey";
    public  static final String encryptionPrivateKeyName = "aesEncryptPrivateKey";
    public  static final String selfEncryptionKeyName = "selfEncryptionKey";

    public static void saveKeys(AtSign atSign, Map<String, String> keys) throws Exception {
        _makeRootFolder();
        File file = getKeysFile(atSign);
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
        Files.writeString(file.toPath(), json);
    }

    private static File getKeysFile(AtSign atSign) {
        return new File(System.getProperty("user.dir") + "/keys/" + atSign + "_key.atKeys");
    }

    public static Map<String, String> loadKeys(AtSign atSign) throws Exception {
        _makeRootFolder();
        File file = getKeysFile(atSign);
        if (! file.exists()) {
            throw new AtException("loadKeys: No file at " + file.getAbsolutePath());
        }

        String json = Files.readString(file.toPath());
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
        Path dir = Paths.get(rootFolder);
        if (! Files.exists(dir)) {
            Files.createDirectories(dir);
        }
    }
}
