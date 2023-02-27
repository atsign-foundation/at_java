package org.atsign.client.api.impl.clients;

import static org.atsign.client.api.AtEvents.AtEventType.decryptedUpdateNotification;

import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.text.ParseException;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;

import org.atsign.client.api.AtClient;
import org.atsign.client.api.AtEvents.AtEventBus;
import org.atsign.client.api.AtEvents.AtEventListener;
import org.atsign.client.api.AtEvents.AtEventType;
import org.atsign.client.api.notification.NotificationService;
import org.atsign.client.api.Secondary;
import org.atsign.client.util.EncryptionUtil;
import org.atsign.client.util.KeysUtil;
import org.atsign.common.AtException;
import org.atsign.common.AtSign;
import org.atsign.common.Keys;
import org.atsign.common.Keys.AtKey;
import org.atsign.common.Keys.Metadata;
import org.atsign.common.Keys.PublicKey;
import org.atsign.common.Keys.SelfKey;
import org.atsign.common.Keys.SharedKey;
import org.atsign.common.ResponseTransformers;
import org.atsign.common.ResponseTransformers.LlookupAllResponseTransformer;
import org.atsign.common.VerbBuilders.DeleteVerbBuilder;
import org.atsign.common.VerbBuilders.LlookupVerbBuilder;
import org.atsign.common.VerbBuilders.PlookupVerbBuilder;
import org.atsign.common.VerbBuilders.ScanVerbBuilder;
import org.atsign.common.VerbBuilders.UpdateVerbBuilder;
import org.atsign.common.options.GetRequestOptions;
import org.atsign.common.response_models.LlookupAllResponse;

/**
 * @see org.atsign.client.api.AtClient
 */
@SuppressWarnings({"RedundantThrows", "unused"})
public class AtClientImpl implements AtClient {
    // Factory method - creates an AtClientImpl with a RemoteSecondary

    private final AtSign atSign;
    @Override public AtSign getAtSign() {return atSign;}

    private final Map<String, String> keys;
    @Override public Map<String, String> getEncryptionKeys() {return keys;}

    private final Secondary secondary;
    @Override public Secondary getSecondary() {return secondary;}

    private final NotificationService notificationService;
    @Override public NotificationService getNotificationService() {return notificationService;}

    private final AtEventBus eventBus;
    public AtClientImpl(AtEventBus eventBus, AtSign atSign, Map<String, String> keys, Secondary secondary) {
        this.eventBus = eventBus;
        this.atSign = atSign;
        this.keys = keys;
        this.secondary = secondary;
        this.notificationService = NotificationService.create(this);
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
    public Response executeCommand(String command, boolean throwExceptionOnErrorResponse) throws AtException {
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
        Response rawResponse = secondary.executeCommand("llookup:" + sharedKey, true);

        try {
            return EncryptionUtil.aesDecryptFromBase64(rawResponse.data, shareEncryptionKey);
        } catch (Exception e) {
            throw new AtException("Failed to " + "decrypt value with shared encryption key" + " : " + e.getMessage(), e);
        }
    }

    private String _getSharedByOtherWithMe(SharedKey sharedKey) throws AtException {
        String what;
        String shareEncryptionKey = getEncryptionKeySharedByOther(sharedKey);

        // first, try to fetch cached - e.g. if I'm @bob, I would first "llookup:cached:@bob:some.key.name@alice"
        what = "llookup cached " + sharedKey;
        Secondary.Response rawResponse = secondary.executeCommand("llookup:cached:" + sharedKey, false);
        if (rawResponse.isError) {
            if (rawResponse.error.contains("AT0015-key not found")) {
                // next, try to fetch from remote - e.g. if I'm @bob, "lookup:some.key.name@alice" - note that "@bob" is NOT required
                String lookupRemoteKey = sharedKey.getFullyQualifiedKeyName() + sharedKey.sharedBy;
                what = "lookup remote " + lookupRemoteKey;
                try {
                    rawResponse = secondary.executeCommand("lookup:" + lookupRemoteKey, true);
                } catch (AtException e) {
                    throw new AtException("Failed to " + what + " : " + rawResponse.error);
                }
            } else {
                throw new AtException("Failed to " + what + " : " + rawResponse.error);
            }
        }

        what = "decrypt value with shared encryption key";
        try {
            return EncryptionUtil.aesDecryptFromBase64(rawResponse.data, shareEncryptionKey);
        } catch (Exception e) {
            throw new AtException("Failed to " + what + " : " + e.getMessage(), e);
        }
    }

    private String _put(SharedKey sharedKey, String value) throws AtException {
        if (! this.atSign.equals(sharedKey.sharedBy)) {
            throw new AtException("sharedBy is [" + sharedKey.sharedBy + "] but should be this client's atSign [" + atSign + "]");
        }
        String what = "";
        try {
            what = "fetch/create shared encryption key";
            String shareToEncryptionKey = getEncryptionKeySharedByMe(sharedKey);

            what = "encrypt value with shared encryption key";
            String cipherText = EncryptionUtil.aesEncryptToBase64(value, shareToEncryptionKey);

            what = "save " + sharedKey + " to secondary";

            String command = "update" + sharedKey.metadata.toString() + ":" + sharedKey + " " + cipherText;
            Secondary.Response response = secondary.executeCommand(command, true);
//            response = secondary.executeCommand(command, true);

            return response.toString();
        } catch (Exception e) {
            throw new AtException("Failed to " + what + " : " + e.getMessage(), e);
        }
    }

    private String _delete(SharedKey sharedKey) throws AtException {
        String what = "";
        try {
            what = "delete " + sharedKey + " from secondary";
            Secondary.Response response = secondary.executeCommand("delete:" + sharedKey, true);

            return response.toString();
        } catch (Exception e) {
            throw new AtException("Failed to " + what + " : " + e.getMessage(), e);
        }
    }

    private String _get(SelfKey key) throws AtException {
        // 1. build command
        String command = null;
        LlookupVerbBuilder builder = new LlookupVerbBuilder();
        builder.with(key, LlookupVerbBuilder.Type.ALL);
        command = builder.build();

        // 2. execute command
        Secondary.Response rawResponse = secondary.executeCommand(command, false);
        if (rawResponse.isError) {
            throw new AtException("Failed to " + command + " : " + rawResponse.error);
        }

        // 3. transform the data to a LlookupAllResponse object
        LlookupAllResponse model = null;
        LlookupAllResponseTransformer transformer = new LlookupAllResponseTransformer();
        model = transformer.transform(rawResponse); // contains variables for data we want from the `llookup:all:<fullKeyName>` command (e.g. model.metaData.ttl)

        // 3. decrypt the value
        String decryptedValue = null;
        String encryptedValue = model.data;
        String selfEncryptionKey = keys.get(KeysUtil.selfEncryptionKeyName);
        try {
            decryptedValue = EncryptionUtil.aesDecryptFromBase64(encryptedValue, selfEncryptionKey);
        } catch (NoSuchPaddingException | NoSuchAlgorithmException | InvalidAlgorithmParameterException | InvalidKeyException | IllegalBlockSizeException | BadPaddingException | NoSuchProviderException e) {
            throw new AtException("Failed to " + command + " : " + e.getMessage(), e);
        }

        // 4. update metadata. squash the fetchedMetadata with current key.metadata (fetchedMetadata has higher priority)
        Metadata fetchedMetadata = Metadata.fromModel(model.metaData);
        key.metadata = Metadata.squash(fetchedMetadata, key.metadata);

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
            throw new AtException("Failed to encrypt value with self encryption key : " + e.getMessage(), e);
        }

        // 3. update secondary
        UpdateVerbBuilder builder = new UpdateVerbBuilder();
        builder.with(selfKey, cipherText);
        String command = builder.build();
        Secondary.Response response = secondary.executeCommand(command, true);
        if(response.isError) {
            throw new AtException("Failed to update " + selfKey + " : " + response.error);
        }
        return response.toString();
    }

    private String _delete(SelfKey key) throws AtException {
        // 1. build delete command
        DeleteVerbBuilder builder = new DeleteVerbBuilder();
        builder.with(key);
        String command = builder.build();

        // 2. run command
        Secondary.Response response = secondary.executeCommand(command, true);
        if(response.isError) {
            throw new AtException("Failed to run command " + command + " : " + response.error);
        }
        return response.toString();
    }

    private String _get(PublicKey key) throws AtException {
        return _get(key, null);
    }

    private String _get(PublicKey key, GetRequestOptions getRequestOptions) throws AtException {
        // 1. build command
        String command = null;
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
        Secondary.Response llookupAllRawResponse = secondary.executeCommand(command, false);
        if (llookupAllRawResponse.isError) {
            throw new AtException("Failed to " + command + " : " + llookupAllRawResponse.error);
        }

        // 3. transform the data to a LlookupAllResponse object
        LlookupAllResponse model = null;
        LlookupAllResponseTransformer transformer = new LlookupAllResponseTransformer();
        model = transformer.transform(llookupAllRawResponse);

        // 4. update key object metadata
        Metadata fetchedMetadata = Metadata.fromModel(model.metaData);
        key.metadata = Metadata.squash(fetchedMetadata, key.metadata);
        key.metadata.isCached = model.key.contains("cached:");

        // 5. return the AtValue
        return model.data;
    }

    private String _put(PublicKey publicKey, String value) throws AtException {
        // 1. generate dataSignature
        publicKey.metadata.dataSignature = generateSignature(value);

        // 2. build command
        String command = null;
        UpdateVerbBuilder builder = new UpdateVerbBuilder();
        builder.with(publicKey, value);
        command = builder.build();

        // 3. run command
        Secondary.Response rawResponse = secondary.executeCommand(command, false);
        if(rawResponse.isError) {
            throw new AtException(rawResponse.error);
        }
        return rawResponse.toString();
    }

    private String _delete(PublicKey key) throws AtException {
        // 1. build command
        String command = null;
        DeleteVerbBuilder builder = new DeleteVerbBuilder();
        builder.with(key);
        command = builder.build();
        
        // 2. run command
        Secondary.Response rawResponse = secondary.executeCommand(command, false);
        if(rawResponse.isError) {
            throw new AtException(rawResponse.error);
        }
        return rawResponse.toString();
    }

    private byte[] _getBinary(SharedKey sharedKey) throws AtException {throw new RuntimeException("Not Implemented");}
    private byte[] _getBinary(SelfKey selfKey) throws AtException {throw new RuntimeException("Not Implemented");}
    private byte[] _getBinary(PublicKey publicKey) throws AtException {throw new RuntimeException("Not Implemented");}
    private byte[] _getBinary(PublicKey publicKey, GetRequestOptions getRequestOptions) throws AtException {throw new RuntimeException("Not Implemented");}

    private String _put(SharedKey sharedKey, byte[] value) throws AtException {throw new RuntimeException("Not Implemented");}
    private String _put(SelfKey selfKey, byte[] value) throws AtException {throw new RuntimeException("Not Implemented");}
    private String _put(PublicKey publicKey, byte[] value) throws AtException {throw new RuntimeException("Not Implemented");}

    private List<AtKey> _getAtKeys(String regex) throws AtException, ParseException {
        ScanVerbBuilder scanVerbBuilder = new ScanVerbBuilder();
        scanVerbBuilder.setRegex(regex);
        scanVerbBuilder.setShowHidden(true); 
        String command = scanVerbBuilder.build();
        Response scanRawResponse = executeCommand(command, false);
        ResponseTransformers.ScanResponseTransformer scanResponseTransformer = new ResponseTransformers.ScanResponseTransformer();
        List<String> rawArray = scanResponseTransformer.transform(scanRawResponse);
        List<AtKey> atKeys = new ArrayList<>();
        for(String atKeyRaw : rawArray) { // eg atKeyRaw == @bob:phone@alice
            AtKey atKey = Keys.fromString(atKeyRaw);
            Secondary.Response llookupMetaRaw = secondary.executeCommand("llookup:meta:" + atKeyRaw, false);
            atKey.metadata = Metadata.squash(atKey.metadata, Metadata.fromString(llookupMetaRaw)); // atKey.metadata has priority over llookupMetaRaw.data
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
        String command = "";
        String toLookup = "shared_key." + key.sharedWith.withoutPrefix() + atSign;
        try {
            command = "llookup:" + toLookup;
            rawResponse = secondary.executeCommand(command, false);
        } catch (AtException e) {
            throw new AtException("Failed to " + command + " : " + e.getMessage(), e);
        }
        if (rawResponse.isError) {
            if (rawResponse.error.contains("AT0015-key not found")) {
                // No key found - so we should create one
                return createSharedEncryptionKey(key);
            } else {
                throw new AtException("Failed to llookup shared key : " + rawResponse.error);
            }
        }

        // When we stored it, we encrypted it with our encryption public key; so we need to decrypt it now with our encryption private key
        try {
            return EncryptionUtil.rsaDecryptFromBase64(rawResponse.data, keys.get(KeysUtil.encryptionPrivateKeyName));
        } catch (Exception e) {
            throw new AtException("Failed to decrypt " + toLookup + " : " + e.getMessage(), e);
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
        try {
            // First, try to fetch cached - e.g. if I'm @bob, llookup:cached:@bob:shared_key@alice
            String llookupCommand = "llookup:cached:" + sharedSharedKeyName;
            what = llookupCommand;
            Secondary.Response rawResponse = secondary.executeCommand(llookupCommand, false);
            if (rawResponse.isError) {
                if (rawResponse.error.contains("AT0015-key not found")) {
                    // Next, try to fetch from remote - e.g. if I'm @bob, lookup:shared_key@alice
                    String lookupCommand = "lookup:" + "shared_key" + sharedKey.sharedBy;
                    what = lookupCommand;
                    rawResponse = secondary.executeCommand(lookupCommand, false);
                    if (rawResponse.isError) {
                        throw new AtException("Failed to " + what + " : " + rawResponse.error);
                    }
                } else {
                    throw new AtException("Failed to " + what + " : " + rawResponse.error);
                }
            }

            what = "decrypt the shared_key  with our encryption private key";
            String sharedSharedKeyDecryptedValue = EncryptionUtil.rsaDecryptFromBase64(rawResponse.data, keys.get(KeysUtil.encryptionPrivateKeyName));
            keys.put(sharedSharedKeyName, sharedSharedKeyDecryptedValue);

            return sharedSharedKeyDecryptedValue;
        } catch (AtException e) {
            throw e;
        } catch (Exception e) {
            throw new AtException("Failed to " + what + " : " + e.getMessage(), e);
        }
    }

    private String createSharedEncryptionKey(SharedKey sharedKey) throws AtException {
        // We need their public key
        String theirPublicEncryptionKey = getPublicEncryptionKey(sharedKey.sharedWith);
        if (theirPublicEncryptionKey == null) {
            throw new AtException(" public key " + sharedKey.sharedWith + " not found but service is running - maybe that AtSign has not yet been onboarded");
        }

        // Cut an AES key
        String aesKey;
        try {
            aesKey = EncryptionUtil.generateAESKeyBase64();
        } catch (Exception e) {
            throw new AtException("Failed to generate AES key for sharing with " + sharedKey.sharedWith + " : " + e.getMessage(), e);
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
            throw new AtException("Failed to " + what + " : " + e.getMessage(), e);
        }

        return aesKey;
    }

    private String getPublicEncryptionKey(AtSign sharedWith) throws AtException {
        // plookup:publickey@alice
        Secondary.Response rawResponse;
        String command = "";
        try {
            command = "plookup:publickey" + sharedWith;
            rawResponse = secondary.executeCommand(command, false);
        } catch (AtException e) {
            throw new AtException("Failed to " + command + " : " + e.getMessage(), e);
        }
        if (rawResponse.isError) {
            if (rawResponse.error.contains("AT0015-key not found")) {
                return null;
            } else {
                throw new AtException("Failed to plookup public key for " + sharedWith + " : " + rawResponse.error);
            }
        } else {
            return rawResponse.data;
        }
    }

    private String generateSignature(String value) throws AtException {
        String signature = null;
        try {
            signature = EncryptionUtil.signSHA256RSA(value, keys.get(KeysUtil.encryptionPrivateKeyName));
        } catch (Exception e) {
            throw new AtException("Failed to sign value: " + value);
        }
        return signature;
    }
}
