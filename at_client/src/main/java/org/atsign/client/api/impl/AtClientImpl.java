package org.atsign.client.api.impl;

import org.atsign.client.api.AtClient;
import org.atsign.client.api.Secondary;
import org.atsign.common.AtSign;

import static org.atsign.common.Keys.*;

import org.atsign.common.AtException;
import org.atsign.client.util.EncryptionUtil;
import org.atsign.client.util.KeysUtil;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

/**
 * @see org.atsign.client.api.AtClient
 */
@SuppressWarnings({"RedundantThrows", "unused"})
public class AtClientImpl implements AtClient, Secondary.EventListener {
    // Factory method - creates an AtClientImpl with a RemoteSecondary

    private final AtSign atSign;
    @Override public AtSign getAtSign() {return atSign;}

    private final Map<String, String> keys;
    @Override public Map<String, String> getEncryptionKeys() {return keys;}
    private final Secondary secondary;
    @Override public Secondary getSecondary() {return secondary;}

    private final Map<EventListener, Set<EventType>> eventListeners = new HashMap<>();

    public AtClientImpl(AtSign atSign, Map<String, String> keys, Secondary secondary) {
        this.atSign = atSign;
        this.keys = keys;
        this.secondary = secondary;
    }

    @Override
    public synchronized void addEventListener(EventListener listener, Set<EventType> eventTypes) {
        eventListeners.put(listener, eventTypes);
        secondary.addEventListener(
                this,
                new HashSet<>(Arrays.asList(EventType.values()))
        );
    }

    @Override
    public synchronized void  removeEventListener(EventListener listener) {
        eventListeners.remove(listener);
        if (eventListeners.isEmpty()) {
            secondary.removeEventListener(this);
        }
    }

    @SuppressWarnings("DuplicatedCode")
    @Override
    public synchronized void handleEvent(EventType eventType, String eventData) {
        Set<Map.Entry<EventListener, Set<EventType>>> listenerEntries = eventListeners.entrySet();
        for (Map.Entry<EventListener, Set<EventType>> next : listenerEntries) {
            try {
                if (next.getValue().contains(eventType)) {
                    next.getKey().handleEvent(eventType, eventData);
                }
            } catch (Exception e) {
                System.err.println(this.getClass().getSimpleName() + " caught exception from one of its event listeners : " + e.getMessage());
                e.printStackTrace(System.err);
            }
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
        String what = "";
        String shareEncryptionKey;

        try {
            if (sharedKey.sharedBy.toString().equals(atSign.toString())) {
                what = "fetch encryption key that I shared";
                shareEncryptionKey = getShareToEncryptionKey(sharedKey);

                // fetch local - e.g. if I'm @bob, I would first "llookup:@alice:some.key.name@bob"
                what = "llookup " + sharedKey;
                Secondary.Response rawResponse = secondary.executeCommand("llookup:" + sharedKey, true);

                what = "decrypt value with shared encryption key";
                return EncryptionUtil.aesDecryptFromBase64(rawResponse.data, shareEncryptionKey);
            } else {
                what = "fetch encryption key that " + sharedKey.sharedBy + " shared";
                shareEncryptionKey = getShareFromEncryptionKey(sharedKey);

                // first, try to fetch cached - e.g. if I'm @bob, I would first "llookup:cached:@bob:some.key.name@alice"
                what = "lookup cached";
                Secondary.Response rawResponse = secondary.executeCommand("llookup:cached:" + sharedKey, false);
                if (rawResponse.isError) {
                    if (rawResponse.error.contains("AT0015-key not found")) {
                        what = "lookup remote";
                        // next, try to fetch from remote - e.g. if I'm @bob, "lookup:some.key.name@alice" - note that "@bob" is NOT required
                        rawResponse = secondary.executeCommand("lookup:" + sharedKey.getFullyQualifiedKeyName() + sharedKey.sharedBy, true);
                    } else {
                        throw new AtException("Failed to " + what + " : " + rawResponse.error);
                    }
                }

                what = "decrypt value with shared encryption key";
                return EncryptionUtil.aesDecryptFromBase64(rawResponse.data, shareEncryptionKey);
            }

        } catch (AtException e) {
            throw e;
        } catch (Exception e) {
            throw new AtException("Failed to " + what + " : " + e.getMessage(), e);
        }
    }

    private String _put(SharedKey sharedKey, String value) throws AtException {
        String what = "";
        try {
            what = "fetch/create shared encryption key";
            String shareToEncryptionKey = getShareToEncryptionKey(sharedKey);

            what = "encrypt value with shared encryption key";
            String cipherText = EncryptionUtil.aesEncryptToBase64(value, shareToEncryptionKey);

            what = "save " + sharedKey + " to secondary";

            String command = "update" + sharedKey.metadata.toString() + ":" + sharedKey + " " + cipherText;
            Secondary.Response response = secondary.executeCommand(command, true);

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

    private String _get(SelfKey key) throws AtException {throw new RuntimeException("Not Implemented");}
    private String _put(SelfKey publicKey, String value) {
        throw new RuntimeException("Not Implemented");
    }
    private String _delete(SelfKey key) {
        throw new RuntimeException("Not Implemented");
    }

    private String _get(PublicKey key) throws AtException {throw new RuntimeException("Not Implemented");}
    private String _put(PublicKey publicKey, String value) {throw new RuntimeException("Not Implemented");}
    private String _delete(PublicKey key) {
        throw new RuntimeException("Not Implemented");
    }

    private byte[] _getBinary(SharedKey sharedKey) throws AtException {throw new RuntimeException("Not Implemented");}
    private byte[] _getBinary(SelfKey selfKey) throws AtException {throw new RuntimeException("Not Implemented");}
    private byte[] _getBinary(PublicKey publicKey) throws AtException {throw new RuntimeException("Not Implemented");}

    private String _put(SharedKey sharedKey, byte[] value) throws AtException {throw new RuntimeException("Not Implemented");}
    private String _put(SelfKey selfKey, byte[] value) throws AtException {throw new RuntimeException("Not Implemented");}
    private String _put(PublicKey publicKey, byte[] value) throws AtException {throw new RuntimeException("Not Implemented");}

    private List<AtKey> _getAtKeys(String regex) {
        throw new RuntimeException("Not Implemented");
    }

    // ============================================================================================================================================
    // ============================================================================================================================================
    // ============================================================================================================================================

    //
    // Internal utility methods. Will move these to another class later, so that other AtClient implementations can easily use them.
    //
    private String getShareToEncryptionKey(SharedKey key) throws AtException {
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
    private String getShareFromEncryptionKey(SharedKey sharedKey) throws AtException {
        String what = "";
        try {
            // First, try to fetch cached - e.g. if I'm @bob, llookup:cached:@bob:shared_key@alice
            String llookupCommand = "llookup:cached:" + sharedKey.sharedWith + ":" + "shared_key" + sharedKey.sharedBy;
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
            return EncryptionUtil.rsaDecryptFromBase64(rawResponse.data, keys.get(KeysUtil.encryptionPrivateKeyName));
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
}
