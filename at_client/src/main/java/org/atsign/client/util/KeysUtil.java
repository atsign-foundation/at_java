package org.atsign.client.util;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.atsign.common.exceptions.AtClientConfigException;
import org.atsign.common.AtSign;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

public class KeysUtil {
    private static final ObjectMapper mapper = new ObjectMapper();

    public static final String expectedKeysFilesLocation = System.getProperty("user.home") + "/.atsign/keys/";
    public static final String legacyKeysFilesLocation = System.getProperty("user.dir") + "/keys/";
    public static final String keysFileSuffix = "_key.atKeys";

    public static final String pkamPublicKeyName = "aesPkamPublicKey";
    public static final String pkamPrivateKeyName = "aesPkamPrivateKey";
    public static final String encryptionPublicKeyName = "aesEncryptPublicKey";
    public static final String encryptionPrivateKeyName = "aesEncryptPrivateKey";
    public static final String selfEncryptionKeyName = "selfEncryptionKey";

    public static void saveKeys(AtSign atSign, Map<String, String> keys) throws Exception {
        File expectedKeysDirectory = new File(expectedKeysFilesLocation);
        if (! expectedKeysDirectory.exists()) {
            Files.createDirectories(expectedKeysDirectory.toPath());
        }
        File file = getKeysFile(atSign, expectedKeysFilesLocation);
        System.out.println("Saving keys to " + file.getAbsolutePath());

        String selfEncryptionKey = keys.get(selfEncryptionKeyName);

        Map<String, String> encryptedKeys = new TreeMap<>();

        // We encrypt all the keys with the AES self encryption key (which is left unencrypted)
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
        // check first if file exists at canonical location ~/.atsign/keys/$atSign_key.atKeys
        File file = getKeysFile(atSign, expectedKeysFilesLocation);

        if (!file.exists()) {
            // if keys do not exist in root, check in keys sub-directory under current working directory
            file = getKeysFile(atSign, legacyKeysFilesLocation);
            // if file does not exist under current working directory, we're done - can't find the keys file
            if (!file.exists()) {
                throw new AtClientConfigException("loadKeys: No file called " + atSign + keysFileSuffix + " at ~/.atsign/keys or ./keys" +
                        "\t Keys files are expected to be in ~/.atsign/keys/ (canonical location) or ./keys/ (legacy location)");
            }
        }
        String json = new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8);
        @SuppressWarnings("unchecked")
        Map<String, String> encryptedKeys = mapper.readValue(json, Map.class);

        // All the keys are encrypted with the AES self encryption key (which is left unencrypted)
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

    public static File getKeysFile(AtSign atSign, String folderToLookIn) {
        return new File(folderToLookIn + atSign + keysFileSuffix);
    }
}
