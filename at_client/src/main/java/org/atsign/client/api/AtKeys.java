package org.atsign.client.api;

import org.atsign.client.util.EnrollmentId;

import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.Base64;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Data class used to hold an {@link org.atsign.client.api.AtClient}s keys
 */
public class AtKeys {

    /**
     * unique id which is assigned by the at_server at enrollment time, this is associated with a specific
     * application and device and therefore a specific the apkam key pair
     */
    private EnrollmentId enrollmentId;

    /**
     * Public Key used for Authentication Management, once this is stored in the at_server then an
     * {@link org.atsign.client.api.AtClient} is able to authenticate using the corresponding private key.
     */
    private String apkamPublicKey;

    /**
     * Private Key used for Authentication Management, this is used to sign the challenge during
     * authentication with the at_server.
     */
    private String apkamPrivateKey;

    /**
     * Encryption Key used during enrollment. This key is sent as part of the enrollment request (encrypted
     * with the atsigns public encryption key). The process that approves the enrollment request uses this
     * key to encrypt the {@link #selfEncryptKey} and {@link #encryptPrivateKey} in the response that it sends.
     * The process which requested the enrollment can then decrypt and store those keys.
     * This ensures that all {@link AtKeys} got and {@link org.atsign.common.AtSign} share the same
     * {@link #selfEncryptKey} and {@link #encryptPrivateKey}
     */
    private String apkamSymmetricKey;

    /**
     * Encryption Key used to encrypt {@link org.atsign.common.Keys.SelfKey}s and the pkam and encryption key pairs
     * when they are externalised as JSON
     */
    private String selfEncryptKey;

    /**
     * This is used to encrypt the symmetric keys that are used to encrypt {@link org.atsign.common.Keys.SharedKey}s
     * where the shared with {@link org.atsign.common.AtSign} is this {@link org.atsign.common.AtSign}
     */

    private String encryptPublicKey;

    /**
     * This is used to decrypt the symmetric keys that are used to encrypt {@link org.atsign.common.Keys.SharedKey}s
     * where the shared with {@link org.atsign.common.AtSign} is this {@link org.atsign.common.AtSign}
     */
    private String encryptPrivateKey;

    /**
     * Transient cache of other keys
     */
    private Map<String, String> cache = new ConcurrentHashMap<>();

    public boolean hasEnrollmentId() {
        return enrollmentId != null;
    }

    public EnrollmentId getEnrollmentId() {
        return enrollmentId;
    }

    public AtKeys setEnrollmentId(EnrollmentId enrollmentId) {
        this.enrollmentId = enrollmentId;
        return this;
    }

    public String getSelfEncryptKey() {
        return selfEncryptKey;
    }

    public AtKeys setSelfEncryptKey(String key) {
        this.selfEncryptKey = key;
        return this;
    }

    public String getApkamPublicKey() {
        return apkamPublicKey;
    }

    public AtKeys setApkamPublicKey(String key) {
        this.apkamPublicKey = key;
        return this;
    }

    public AtKeys setApkamPublicKey(PublicKey key) {
        return setApkamPublicKey(createStringBase64(key));
    }

    public String getApkamPrivateKey() {
        return apkamPrivateKey;
    }

    public AtKeys setApkamPrivateKey(String key) {
        this.apkamPrivateKey = key;
        return this;
    }

    public AtKeys setApkamPrivateKey(PrivateKey key) {
        return setApkamPrivateKey(createStringBase64(key));
    }

    public AtKeys setApkamKeyPair(KeyPair keyPair) {
        setApkamPublicKey(keyPair.getPublic());
        setApkamPrivateKey(keyPair.getPrivate());
        return this;
    }

    public boolean hasPkamKeys() {
        return this.apkamPublicKey != null && this.apkamPrivateKey != null;
    }

    public AtKeys setEncryptKeyPair(KeyPair keyPair) {
        setEncryptPublicKey(keyPair.getPublic());
        setEncryptPrivateKey(keyPair.getPrivate());
        return this;
    }

    public String getEncryptPublicKey() {
        return encryptPublicKey;
    }

    public AtKeys setEncryptPublicKey(String key) {
        this.encryptPublicKey = key;
        return this;
    }

    public AtKeys setEncryptPublicKey(PublicKey key) {
        return setEncryptPublicKey(createStringBase64(key));
    }

    public String getEncryptPrivateKey() {
        return encryptPrivateKey;
    }

    public AtKeys setEncryptPrivateKey(String key) {
        this.encryptPrivateKey = key;
        return this;
    }

    public AtKeys setEncryptPrivateKey(PrivateKey key) {
        return setEncryptPrivateKey(createStringBase64(key));
    }

    public String getApkamSymmetricKey() {
        return apkamSymmetricKey;
    }

    public AtKeys setApkamSymmetricKey(String key) {
        this.apkamSymmetricKey = key;
        return this;
    }

    public String get(String key) {
        return cache.get(key);
    }

    public void put(String key, String value) {
        cache.put(key, value);
    }

    public Map<String, String> getCache() {
        return Collections.unmodifiableMap(cache);
    }

    private static String createStringBase64(PublicKey key) {
        return Base64.getEncoder().encodeToString(key.getEncoded());
    }

    private static String createStringBase64(PrivateKey key) {
        return Base64.getEncoder().encodeToString(key.getEncoded());
    }

}
