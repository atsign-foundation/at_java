package org.atsign.client.api.impl.clients;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.atsign.client.api.AtClient;
import org.atsign.client.api.AtEvents.AtEventBus;
import org.atsign.client.api.AtEvents.AtEventListener;
import org.atsign.client.api.AtEvents.AtEventType;
import org.atsign.client.api.Secondary;
import org.atsign.client.util.EncryptionUtil;
import org.atsign.client.util.KeysUtil;
import org.atsign.common.*;
import org.atsign.common.Keys.AtKey;
import org.atsign.common.Keys.PublicKey;
import org.atsign.common.Keys.SelfKey;
import org.atsign.common.Keys.SharedKey;
import org.atsign.common.VerbBuilders.*;
import org.atsign.common.exceptions.*;
import org.atsign.common.options.GetRequestOptions;
import org.atsign.common.response_models.LookupResponse;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import java.io.IOException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.time.OffsetDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

import static org.atsign.client.api.AtEvents.AtEventType.decryptedUpdateNotification;

/**
 * @see org.atsign.client.api.AtClient
 */
@SuppressWarnings({"RedundantThrows", "unused"})
public class AtClientImpl implements AtClient {
    static final ObjectMapper json = new ObjectMapper();

    // Factory method - creates an AtClientImpl with a RemoteSecondary

    private final AtSign atSign;
    @Override public AtSign getAtSign() {return atSign;}

    private final Map<String, String> keys;
    @Override public Map<String, String> getEncryptionKeys() {return keys;}
    private final Secondary secondary;
    @Override public Secondary getSecondary() {return secondary;}

    private final AtEventBus eventBus;
    public AtClientImpl(AtEventBus eventBus, AtSign atSign, Map<String, String> keys, Secondary secondary) {
        this.eventBus = eventBus;
        this.atSign = atSign;
        this.keys = keys;
        this.secondary = secondary;

        eventBus.addEventListener(this, EnumSet.allOf(AtEventType.class));
    }

    @Override public void startMonitor() {secondary.startMonitor();}
    @Override public void stopMonitor() {secondary.stopMonitor();}
    @Override public boolean isMonitorRunning() {return secondary.isMonitorRunning();}

    @Override
    public synchronized void addEventListener(AtEventListener listener, Set<AtEventType> eventTypes) {
        eventBus.addEventListener(listener, eventTypes);
    }

    @Override
    public synchronized void  removeEventListener(AtEventListener listener) {
        eventBus.removeEventListener(listener);
    }

    @Override
    public int publishEvent(AtEventType eventType, Map<String, Object> eventData) {
        return eventBus.publishEvent(eventType, eventData);
    }

    @Override
    public synchronized void handleEvent(AtEventType eventType, Map<String, Object> eventData) {
        switch (eventType) {
            case sharedKeyNotification: {
                // We've got notification that someone has shared an encryption key with us
                // If we also got a value, we can decrypt it and add it to our keys map
                // Note: a value isn't supplied when the ttr on the shared key was set to 0
                if (eventData.get("value") != null) {
                    String sharedSharedKeyName = (String) eventData.get("key");
                    String sharedSharedKeyEncryptedValue = (String) eventData.get("value");
                    // decrypt it with our encryption private key
                    try {
                        String sharedKeyDecryptedValue = EncryptionUtil.rsaDecryptFromBase64(sharedSharedKeyEncryptedValue, keys.get(KeysUtil.encryptionPrivateKeyName));
                        keys.put(sharedSharedKeyName, sharedKeyDecryptedValue);
                    } catch (Exception e) {
                        System.err.println(OffsetDateTime.now() + ": caught exception " + e + " while decrypting received shared key " + sharedSharedKeyName);
                    }
                }
            }
            break;
            case updateNotification: {
                // Let's see if we can decrypt it on the fly
                if (eventData.get("value") != null) {
                    String key = (String) eventData.get("key");
                    String encryptedValue = (String) eventData.get("value");

                    try {
                        // decrypt it with the symmetric key that the other atSign shared with me
                        String encryptionKeySharedByOther = getEncryptionKeySharedByOther(SharedKey.fromString(key));

                        String decryptedValue = EncryptionUtil.aesDecryptFromBase64(encryptedValue, encryptionKeySharedByOther);
                        HashMap<String, Object> newEventData = new HashMap<>(eventData);
                        newEventData.put("decryptedValue", decryptedValue);
                        eventBus.publishEvent(decryptedUpdateNotification, newEventData);
                    } catch (Exception e) {
                        System.err.println(OffsetDateTime.now() + ": caught exception " + e + " while decrypting received data with key name [" + key + "]");
                    }
                }
            }
            break;
            default:
                break;
        }
    }

    @Override
    public CompletableFuture<String> get(SharedKey sharedKey) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return _get(sharedKey);
            } catch (Exception e) {
                throw new CompletionException(e);
            }
        });
    }

    @Override
    public CompletableFuture<byte[]> getBinary(SharedKey sharedKey) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return _getBinary(sharedKey);
            } catch (Exception e) {
                throw new CompletionException(e);
            }
        });
    }

    @Override
    public CompletableFuture<String> put(SharedKey sharedKey, String value) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return _put(sharedKey, value);
            } catch (Exception e) {
                throw new CompletionException(e);
            }
        });
    }

    @Override
    public CompletableFuture<String> delete(SharedKey sharedKey) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return _delete(sharedKey);
            } catch (Exception e) {
                throw new CompletionException(e);
            }
        });
    }

    @Override
    public CompletableFuture<String> get(SelfKey selfKey) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return _get(selfKey);
            } catch (Exception e) {
                throw new CompletionException(e);
            }
        });
    }

    @Override
    public CompletableFuture<byte[]> getBinary(SelfKey selfKey) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return _getBinary(selfKey);
            } catch (Exception e) {
                throw new CompletionException(e);
            }
        });
    }

    @Override
    public CompletableFuture<String> put(SelfKey selfKey, String value) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return _put(selfKey, value);
            } catch (Exception e) {
                throw new CompletionException(e);
            }
        });
    }

    @Override
    public CompletableFuture<String> delete(SelfKey selfKey) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return _delete(selfKey);
            } catch (Exception e) {
                throw new CompletionException(e);
            }
        });
    }

    @Override
    public CompletableFuture<String> get(PublicKey publicKey) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return _get(publicKey);
            } catch (Exception e) {
                throw new CompletionException(e);
            }
        });
    }

    @Override
    public CompletableFuture<String> get(PublicKey publicKey, GetRequestOptions getRequestOptions) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return _get(publicKey, getRequestOptions);
            } catch (Exception e) {
                throw new CompletionException(e);
            }
        });
    }
    
    @Override
    public CompletableFuture<byte[]> getBinary(PublicKey publicKey) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return _getBinary(publicKey);
            } catch (Exception e) {
                throw new CompletionException(e);
            }
        });
    }

    @Override
    public CompletableFuture<byte[]> getBinary(PublicKey publicKey, GetRequestOptions getRequestOptions) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return _getBinary(publicKey, getRequestOptions);
            } catch (Exception e) {
                throw new CompletionException(e);
            }
        });
    }

    @Override
    public CompletableFuture<String> put(PublicKey publicKey, String value) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return _put(publicKey, value);
            } catch (Exception e) {
                throw new CompletionException(e);
            }
        });
    }

    @Override
    public CompletableFuture<String> delete(PublicKey publicKey) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return _delete(publicKey);
            } catch (Exception e) {
                throw new CompletionException(e);
            }
        });
    }

    @Override
    public CompletableFuture<String> put(SharedKey sharedKey, byte[] value) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return _put(sharedKey, value);
            } catch (Exception e) {
                throw new CompletionException(e);
            }
        });
    }

    @Override
    public CompletableFuture<String> put(SelfKey selfKey, byte[] value) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return _put(selfKey, value);
            } catch (Exception e) {
                throw new CompletionException(e);
            }
        });
    }

    @Override
    public CompletableFuture<String> put(PublicKey publicKey, byte[] value) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return _put(publicKey, value);
            } catch (Exception e) {
                throw new CompletionException(e);
            }
        });
    }

    @Override
    public CompletableFuture<List<AtKey>> getAtKeys(String regex) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return _getAtKeys(regex);
            } catch (Exception e) {
                throw new CompletionException(e);
            }
        });
    }

    /**
     * Synchronous, talks @-protocol directly to the client's Secondary server
     * @param command in @ protocol format
     * @param throwExceptionOnErrorResponse sometimes we want to inspect an error response,
     *        sometimes we want to just throw an exception
     * @return a Secondary Response
     * @throws AtException if the response from the Secondary starts with 'error:', or
     * if there is any other exception
     */
    @Override
    public Response executeCommand(String command, boolean throwExceptionOnErrorResponse) throws AtException, IOException {
        return secondary.executeCommand(command, throwExceptionOnErrorResponse);
    }

// ============================================================================================================================================
    // ============================================================================================================================================
    // ============================================================================================================================================

    //
    // Synchronous methods which do the actual work
    //
    private String _get(SharedKey sharedKey) throws AtException {
        if (sharedKey.sharedBy.toString().equals(atSign.toString())) {
            return _getSharedByMeWithOther(sharedKey);
        } else {
            return _getSharedByOtherWithMe(sharedKey);
        }
    }

    private String _getSharedByMeWithOther(SharedKey sharedKey) throws AtException {
        String shareEncryptionKey = getEncryptionKeySharedByMe(sharedKey);

        // fetch local - e.g. if I'm @bob, I would first "llookup:@alice:some.key.name@bob"
        Response rawResponse;
        String command = "llookup:" + sharedKey;
        try {
            rawResponse = secondary.executeCommand(command, true);
        } catch (IOException e) {
            throw new AtSecondaryConnectException("Failed to execute " + command, e);
        }

        try {
            return EncryptionUtil.aesDecryptFromBase64(rawResponse.getRawDataResponse(), shareEncryptionKey);
        } catch (Exception e) {
            throw new AtDecryptionException("Failed to decrypt value with shared encryption key", e);
        }
    }

    private String _getSharedByOtherWithMe(SharedKey sharedKey) throws AtException {
        String what;
        String shareEncryptionKey = getEncryptionKeySharedByOther(sharedKey);

        Response rawResponse;
        String command = "lookup:" + sharedKey;
        try {
            rawResponse = secondary.executeCommand(command, true);
        } catch (IOException e) {
            throw new AtSecondaryConnectException("Failed to execute " + command, e);
        }

        what = "decrypt value with shared encryption key";
        try {
            return EncryptionUtil.aesDecryptFromBase64(rawResponse.getRawDataResponse(), shareEncryptionKey);
        } catch (Exception e) {
            throw new AtDecryptionException("Failed to " + what, e);
        }
    }

    private String _put(SharedKey sharedKey, String value) throws AtException {
        if (! this.atSign.equals(sharedKey.sharedBy)) {
            throw new AtIllegalArgumentException("sharedBy is [" + sharedKey.sharedBy + "] but should be this client's atSign [" + atSign + "]");
        }
        String what = "";
        String cipherText;
        try {
            what = "fetch/create shared encryption key";
            String shareToEncryptionKey = getEncryptionKeySharedByMe(sharedKey);

            what = "encrypt value with shared encryption key";
            cipherText = EncryptionUtil.aesEncryptToBase64(value, shareToEncryptionKey);
        } catch (Exception e) {
            throw new AtEncryptionException("Failed to " + what, e);
        }

        String command = "update" + sharedKey.metadata.toString() + ":" + sharedKey + " " + cipherText;

        try {
            return secondary.executeCommand(command, true).toString();
        } catch (IOException e) {
            throw new AtSecondaryConnectException("Failed to execute " + command, e);
        }
    }

    private String _delete(SharedKey sharedKey) throws AtException {
        String command = "delete:" + sharedKey;
        try {
            return secondary.executeCommand(command, true).toString();
        } catch (IOException e) {
            throw new AtSecondaryConnectException("Failed to execute " + command, e);
        }
    }

    private String _get(SelfKey key) throws AtException {
        // 1. build command
        String command;
        LlookupVerbBuilder builder = new LlookupVerbBuilder();
        builder.with(key, LlookupVerbBuilder.Type.ALL);
        command = builder.build();

        // 2. execute command
        Response response;
        try {
            response = secondary.executeCommand(command, true);
        } catch (IOException e) {
            throw new AtSecondaryConnectException("Failed to execute " + command, e);
        }

        // 3. transform the data to a LlookupAllResponse object
        LookupResponse fetched;
        try {
            fetched = json.readValue(response.getRawDataResponse(), LookupResponse.class);
        } catch (JsonProcessingException e) {
            throw new AtResponseHandlingException("Failed to parse JSON " + response.getRawDataResponse(), e);
        }

        // 3. decrypt the value
        String decryptedValue;
        String encryptedValue = fetched.data;
        String selfEncryptionKey = keys.get(KeysUtil.selfEncryptionKeyName);
        try {
            decryptedValue = EncryptionUtil.aesDecryptFromBase64(encryptedValue, selfEncryptionKey);
        } catch (NoSuchPaddingException | NoSuchAlgorithmException | InvalidAlgorithmParameterException | InvalidKeyException | IllegalBlockSizeException | BadPaddingException | NoSuchProviderException e) {
            throw new AtDecryptionException("Failed to " + command, e);
        }

        // 4. update metadata. squash the fetchedMetadata with current key.metadata (fetchedMetadata has higher priority)
        key.metadata = Metadata.squash(fetched.metaData, key.metadata);

        return decryptedValue;
    }

    private String _put(SelfKey selfKey, String value) throws AtException {
        // 1. generate dataSignature
        selfKey.metadata.dataSignature = generateSignature(value);

        // 2. encrypt data with self encryption key
        String cipherText;
        try {
            cipherText = EncryptionUtil.aesEncryptToBase64(value, keys.get(KeysUtil.selfEncryptionKeyName));
        } catch (NoSuchPaddingException | NoSuchAlgorithmException | InvalidAlgorithmParameterException | InvalidKeyException | IllegalBlockSizeException | BadPaddingException | NoSuchProviderException e) {
            throw new AtEncryptionException("Failed to encrypt value with self encryption key", e);
        }

        // 3. update secondary
        UpdateVerbBuilder builder = new UpdateVerbBuilder();
        builder.with(selfKey, cipherText);
        String command = builder.build();
        try {
            return secondary.executeCommand(command, true).toString();
        } catch (IOException e) {
            throw new AtSecondaryConnectException("Failed to execute " + command, e);
        }
    }

    private String _delete(SelfKey key) throws AtException {
        // 1. build delete command
        DeleteVerbBuilder builder = new DeleteVerbBuilder();
        builder.with(key);
        String command = builder.build();

        // 2. run command
        try {
            return secondary.executeCommand(command, true).toString();
        } catch (IOException e) {
            throw new AtSecondaryConnectException("Failed to execute " + command, e);
        }
    }

    private String _get(PublicKey key) throws AtException {
        return _get(key, null);
    }

    private String _get(PublicKey key, GetRequestOptions getRequestOptions) throws AtException {
        // 1. build command
        String command;
        if(atSign.toString().equals(key.sharedBy.toString())) {
            // it's a public key created by this client => llookup
            LlookupVerbBuilder builder = new LlookupVerbBuilder();
            builder.with(key, LlookupVerbBuilder.Type.ALL);
            command = builder.build();
        } else {
            // it's a public key created by another => plookup
            PlookupVerbBuilder builder = new PlookupVerbBuilder();
            builder.with(key, PlookupVerbBuilder.Type.ALL);
            builder.setBypassCache(getRequestOptions != null && getRequestOptions.getBypassCache());
            command = builder.build();
        }

        // 2. run the command
        Response response;
        try {
            response = secondary.executeCommand(command, true);
        } catch (IOException e) {
            throw new AtSecondaryConnectException("Failed to execute " + command, e);
        }

        // 3. transform the data to a LlookupAllResponse object
        LookupResponse fetched;
        try {
            fetched = json.readValue(response.getRawDataResponse(), LookupResponse.class);
        } catch (JsonProcessingException e) {
            throw new AtResponseHandlingException("Failed to parse JSON " + response.getRawDataResponse(), e);
        }

        // 4. update key object metadata
        key.metadata = Metadata.squash(fetched.metaData, key.metadata);
        key.metadata.isCached = fetched.key.contains("cached:");

        // 5. return the AtValue
        return fetched.data;
    }

    private String _put(PublicKey publicKey, String value) throws AtException {
        // 1. generate dataSignature
        publicKey.metadata.dataSignature = generateSignature(value);

        // 2. build command
        String command;
        UpdateVerbBuilder builder = new UpdateVerbBuilder();
        builder.with(publicKey, value);
        command = builder.build();

        // 3. run command
        try {
            return secondary.executeCommand(command, true).toString();
        } catch (IOException e) {
            throw new AtSecondaryConnectException("Failed to execute " + command, e);
        }
    }

    private String _delete(PublicKey key) throws AtException {
        // 1. build command
        String command;
        DeleteVerbBuilder builder = new DeleteVerbBuilder();
        builder.with(key);
        command = builder.build();
        
        // 2. run command
        try {
            return secondary.executeCommand(command, true).toString();
        } catch (IOException e) {
            throw new AtSecondaryConnectException("Failed to execute " + command, e);
        }
    }

    private byte[] _getBinary(SharedKey sharedKey) throws AtException {throw new RuntimeException("Not Implemented");}
    private byte[] _getBinary(SelfKey selfKey) throws AtException {throw new RuntimeException("Not Implemented");}
    private byte[] _getBinary(PublicKey publicKey) throws AtException {throw new RuntimeException("Not Implemented");}
    private byte[] _getBinary(PublicKey publicKey, GetRequestOptions getRequestOptions) throws AtException {throw new RuntimeException("Not Implemented");}

    private String _put(SharedKey sharedKey, byte[] value) throws AtException {throw new RuntimeException("Not Implemented");}
    private String _put(SelfKey selfKey, byte[] value) throws AtException {throw new RuntimeException("Not Implemented");}
    private String _put(PublicKey publicKey, byte[] value) throws AtException {throw new RuntimeException("Not Implemented");}

    private List<AtKey> _getAtKeys(String regex) throws AtException {
        ScanVerbBuilder scanVerbBuilder = new ScanVerbBuilder();
        scanVerbBuilder.setRegex(regex);
        scanVerbBuilder.setShowHidden(true); 
        String scanCommand = scanVerbBuilder.build();
        Response scanRawResponse;
        try {
            scanRawResponse = executeCommand(scanCommand, true);
        } catch (IOException e) {
            throw new AtSecondaryConnectException("Failed to execute " + scanCommand, e);
        }
        ResponseTransformers.ScanResponseTransformer scanResponseTransformer = new ResponseTransformers.ScanResponseTransformer();
        List<String> rawArray = scanResponseTransformer.transform(scanRawResponse);
        List<AtKey> atKeys = new ArrayList<>();
        for(String atKeyRaw : rawArray) { // eg atKeyRaw == @bob:phone@alice
            AtKey atKey = Keys.fromString(atKeyRaw);
            String llookupCommand = "llookup:meta:" + atKeyRaw;
            Response llookupMetaResponse;
            try {
                llookupMetaResponse = secondary.executeCommand(llookupCommand, true);
            } catch (IOException e) {
                throw new AtSecondaryConnectException("Failed to execute " + llookupCommand, e);
            }
            try {
                atKey.metadata = Metadata.squash(atKey.metadata, Metadata.fromJson(llookupMetaResponse.getRawDataResponse())); // atKey.metadata has priority over llookupMetaRaw.data
            } catch (JsonProcessingException e) {
                throw new AtResponseHandlingException("Failed to parse JSON " + llookupMetaResponse.getRawDataResponse(), e);
            }
            atKeys.add(atKey);
        }
        return atKeys;
    }

    // ============================================================================================================================================
    // ============================================================================================================================================
    // ============================================================================================================================================

    //
    // Internal utility methods. Will move these to another class later, so that other AtClient implementations can easily use them.
    //
    private String getEncryptionKeySharedByMe(SharedKey key) throws AtException {
        // llookup:shared_key.bob@alice
        Secondary.Response rawResponse;
        String toLookup = "shared_key." + key.sharedWith.withoutPrefix() + atSign;

        String command = "llookup:" + toLookup;
        try {
            rawResponse = secondary.executeCommand(command, false);
        } catch (IOException e) {
            throw new AtSecondaryConnectException("Failed to execute " + command, e);
        }

        if (rawResponse.isError()) {
            if (rawResponse.getException() instanceof AtKeyNotFoundException) {
                // No key found - so we should create one
                return createSharedEncryptionKey(key);
            } else {
                throw rawResponse.getException();
            }
        }

        // When we stored it, we encrypted it with our encryption public key; so we need to decrypt it now with our encryption private key
        try {
            return EncryptionUtil.rsaDecryptFromBase64(rawResponse.getRawDataResponse(), keys.get(KeysUtil.encryptionPrivateKeyName));
        } catch (Exception e) {
            throw new AtDecryptionException("Failed to decrypt " + toLookup, e);
        }
    }
    private String getEncryptionKeySharedByOther(SharedKey sharedKey) throws AtException {
        // Let's see if it's in our in-memory cache
        String sharedSharedKeyName = sharedKey.getSharedSharedKeyName();

        String sharedKeyValue = keys.get(sharedSharedKeyName);
        if (sharedKeyValue != null) {
            return sharedKeyValue;
        }

        String what = "";

        // Not in memory so now let's try to fetch from remote - e.g. if I'm @bob, lookup:shared_key@alice
        String lookupCommand = "lookup:" + "shared_key" + sharedKey.sharedBy;
        Response rawResponse;
        try {
            rawResponse = secondary.executeCommand(lookupCommand, true);
        } catch (IOException e) {
            throw new AtSecondaryConnectException("Failed to execute " + lookupCommand, e);
        }

        String sharedSharedKeyDecryptedValue;
        try {
            sharedSharedKeyDecryptedValue = EncryptionUtil.rsaDecryptFromBase64(rawResponse.getRawDataResponse(), keys.get(KeysUtil.encryptionPrivateKeyName));
        } catch (Exception e) {
            throw new AtDecryptionException("Failed to decrypt the shared_key with our encryption private key", e);
        }
        keys.put(sharedSharedKeyName, sharedSharedKeyDecryptedValue);

        return sharedSharedKeyDecryptedValue;
    }

    private String createSharedEncryptionKey(SharedKey sharedKey) throws AtException {
        // We need their public key
        String theirPublicEncryptionKey = getPublicEncryptionKey(sharedKey.sharedWith);
        if (theirPublicEncryptionKey == null) {
            throw new AtKeyNotFoundException(" public key " + sharedKey.sharedWith + " not found but service is running - maybe that AtSign has not yet been onboarded");
        }

        // Cut an AES key
        String aesKey;
        try {
            aesKey = EncryptionUtil.generateAESKeyBase64();
        } catch (Exception e) {
            throw new AtEncryptionException("Failed to generate AES key for sharing with " + sharedKey.sharedWith, e);
        }

        String what = "";
        try {
            // Encrypt key with the other at-sign's publickey and save it @bob:shared_key@alice
            what = "encrypt new shared key with their public key";
            String encryptedForOther = EncryptionUtil.rsaEncryptToBase64(aesKey, theirPublicEncryptionKey);

            what = "encrypt new shared key with our public key";
            // Encrypt key with our publickey and save it shared_key.bob@alice
            String encryptedForUs = EncryptionUtil.rsaEncryptToBase64(aesKey, keys.get(KeysUtil.encryptionPublicKeyName));

            what = "save encrypted shared key for us";
            secondary.executeCommand("update:" + "shared_key." + sharedKey.sharedWith.withoutPrefix() + sharedKey.sharedBy
                    + " " + encryptedForUs, true);

            what = "save encrypted shared key for them";
            int ttr = 24 * 60 * 60 * 1000;
            secondary.executeCommand("update:ttr:" + ttr + ":" + sharedKey.sharedWith + ":shared_key" + sharedKey.sharedBy
                    + " " + encryptedForOther, true);
        } catch (Exception e) {
            throw new AtEncryptionException("Failed to " + what, e);
        }

        return aesKey;
    }

    private String getPublicEncryptionKey(AtSign sharedWith) throws AtException {
        // plookup:publickey@alice
        Secondary.Response rawResponse;

        String command = "plookup:publickey" + sharedWith;
        try {
            rawResponse = secondary.executeCommand(command, false);
        } catch (IOException e) {
            throw new AtSecondaryConnectException("Failed to execute " + command, e);
        }

        if (rawResponse.isError()) {
            if (rawResponse.getException() instanceof AtKeyNotFoundException) {
                return null;
            } else {
                throw rawResponse.getException();
            }
        } else {
            return rawResponse.getRawDataResponse();
        }
    }

    private String generateSignature(String value) throws AtException {
        String signature;
        try {
            signature = EncryptionUtil.signSHA256RSA(value, keys.get(KeysUtil.encryptionPrivateKeyName));
        } catch (Exception e) {
            throw new AtEncryptionException("Failed to sign value: " + value, e);
        }
        return signature;
    }
}
