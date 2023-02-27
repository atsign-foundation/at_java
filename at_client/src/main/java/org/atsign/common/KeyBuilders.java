package org.atsign.common;

import org.atsign.common.exceptions.AtInvalidAtKeyException;

import static org.atsign.common.Keys.*;

@SuppressWarnings({"unused", "UnusedReturnValue"})
public class KeyBuilders {
    public interface KeyBuilder {
        KeyBuilder timeToLive(int ttl);
        KeyBuilder timeToBirth(int ttb);
        Keys.AtKey build();
        void validate() throws AtException;
        KeyBuilder namespace(String namespace);
    }
    public static abstract class BaseKeyBuilder {
        AtKey _atKey;

        /// Set simple key without any namespace. For example "phone", "email" etc...
        /// This is required.
        public BaseKeyBuilder key(String key) {
            key = key.trim();
            _atKey.name = key;
            return this;
        }

        /// Each app should write to a specific namespace.
        /// This is required, unless the key already includes some '.' delimiters
        public BaseKeyBuilder namespace(String namespace) {
            namespace = namespace.trim();
            _atKey.setNamespace(namespace);
            return this;
        }

        /// Set this value to set an expiry to a key in milliseconds.
        /// Time until expiry
        public BaseKeyBuilder timeToLive(int ttl) {
            _atKey.metadata.ttl = ttl;
            return this;
        }

        /// Set this value to set time after which the key should be available in milliseconds.
        /// Time until availability
        public BaseKeyBuilder timeToBirth(int ttb) {
            _atKey.metadata.ttb = ttb;
            return this;
        }

        /// Returns an instance of AtKey
        public Keys.AtKey build() throws AtException {
            // Validate if the data is set properly
            validate();

            return _atKey;
        }

        /// Validates AtKey and throws Exception for a given issue
        public void validate() throws AtException {
            if (_atKey.name == null || _atKey.name.isEmpty()) {
                throw new AtInvalidAtKeyException("Key cannot be empty");
            }

            // We only need to check namespace if the key already doesn't include a namespace
            if (! _atKey.name.contains(".")) {
                if (_atKey.getNamespace() == null || _atKey.getNamespace().isEmpty()) {
                    throw new AtInvalidAtKeyException("Namespace cannot be empty");
                }
            }
        }
    }

    /**
     * Builder class for cacheable keys.
     */
    public static abstract class CachedKeyBuilder extends BaseKeyBuilder {
        /**
         * <ul>
         * <li>Cacheable keys are cached on the recipient AtSign's secondary server when the
         * ttr metadata value is set to a value greater than zero.</li>
         * <li>TTR denotes the time to refresh the cached key. Accepts an integer value
         * which represents the time units in seconds.</li>
         * <li>CCD denotes the cascade delete. Accepts a boolean value. When set to true, deletes
         * the cached key when corresponding key is deleted. When set to false, the cached key remains
         * when corresponding key is deleted.</li>
         * </ul>
         */
        public CachedKeyBuilder cache(int ttr, boolean ccd) {
            _atKey.metadata.ttr = ttr;
            _atKey.metadata.ccd = ccd;
            _atKey.metadata.isCached = (ttr != 0);
            return this;
        }
    }

    /// Builder to build the public keys
    public static class PublicKeyBuilder extends CachedKeyBuilder implements KeyBuilder {
        public PublicKeyBuilder() {
            this(defaultAtSign);
        }
        public PublicKeyBuilder(AtSign sharedBy) {
            _atKey = new PublicKey(sharedBy);
            _atKey.metadata.isPublic = true;
            _atKey.metadata.isHidden = false;
        }

        @Override
        public PublicKeyBuilder key(String key) {
            super.key(key);
            return this;
        }

        @Override
        public PublicKeyBuilder namespace(String namespace) {
            super.namespace(namespace);
            return this;
        }

        @Override
        public PublicKeyBuilder timeToLive(int ttl) {
            super.timeToLive(ttl);
            return this;
        }

        @Override
        public PublicKeyBuilder timeToBirth(int ttb) {
            super.timeToBirth(ttb);
            return this;
        }

        @Override
        public PublicKey build() {
            return (PublicKey) _atKey;
        }

        @Override
        public PublicKeyBuilder cache(int ttr, boolean ccd) {
            super.cache(ttr, ccd);
            return this;
        }
    }

    /// Builder to build the shared keys
    public static class SharedKeyBuilder extends CachedKeyBuilder implements KeyBuilder {
        public SharedKeyBuilder(AtSign sharedWith) {
            this(defaultAtSign, sharedWith);
        }
        public SharedKeyBuilder(AtSign sharedBy, AtSign sharedWith) {
            _atKey = new SharedKey(sharedBy, sharedWith);
            _atKey.metadata.isPublic = false;
            _atKey.metadata.isHidden = false;
        }

        /// Accepts a string which represents an atSign for the key is created.
        void sharedWith(AtSign sharedWith) {
            _atKey.sharedWith = sharedWith;
        }

        @Override
        public SharedKeyBuilder key(String key) {
            super.key(key);
            return this;
        }

        @Override
        public SharedKeyBuilder namespace(String namespace) {
            super.namespace(namespace);
            return this;
        }

        @Override
        public SharedKeyBuilder timeToLive(int ttl) {
            super.timeToLive(ttl);
            return this;
        }

        @Override
        public SharedKeyBuilder timeToBirth(int ttb) {
            super.timeToBirth(ttb);
            return this;
        }

        @Override
        public SharedKey build() {
            return (SharedKey) _atKey;
        }

        @Override
        public SharedKeyBuilder cache(int ttr, boolean ccd) {
            super.cache(ttr, ccd);
            return this;
        }

        @Override
        public void validate() throws AtException {
            //Call AbstractKeyBuilder validate method to perform the common validations.
            super.validate();
            if (_atKey.sharedWith == null || _atKey.sharedWith.toString().isEmpty()) {
                throw new AtInvalidAtKeyException("sharedWith cannot be empty");
            }
        }
    }

    /// Builder to build the Self keys
    public static class SelfKeyBuilder extends BaseKeyBuilder implements KeyBuilder {
        public SelfKeyBuilder() {
            this(defaultAtSign);
        }
        public SelfKeyBuilder(AtSign sharedBy) {
            this(sharedBy, null);
        }

        public SelfKeyBuilder(AtSign sharedBy, AtSign sharedWith) {
            _atKey = new SelfKey(sharedBy, sharedWith);
            _atKey.metadata.isPublic = false;
            _atKey.metadata.isHidden = false;
        }

        @Override
        public SelfKeyBuilder key(String key) {
            super.key(key);
            return this;
        }

        @Override
        public SelfKeyBuilder namespace(String namespace) {
            super.namespace(namespace);
            return this;
        }

        @Override
        public SelfKeyBuilder timeToLive(int ttl) {
            super.timeToLive(ttl);
            return this;
        }

        @Override
        public SelfKeyBuilder timeToBirth(int ttb) {
            super.timeToBirth(ttb);
            return this;
        }

        @Override
        public SelfKey build() {
            return (SelfKey) _atKey;
        }
    }

    /// Builder to build the hidden keys
    public static class PrivateHiddenKeyBuilder extends BaseKeyBuilder {
        public PrivateHiddenKeyBuilder() {
            this(defaultAtSign);
        }
        public PrivateHiddenKeyBuilder(AtSign sharedBy) {
            _atKey = new PrivateHiddenKey(sharedBy);
            _atKey.metadata.isHidden = true;
            _atKey.metadata.isPublic = false;
        }

        @Override
        public PrivateHiddenKeyBuilder key(String key) {
            super.key(key);
            return this;
        }

        @Override
        public PrivateHiddenKeyBuilder namespace(String namespace) {
            super.namespace(namespace);
            return this;
        }

        @Override
        public PrivateHiddenKeyBuilder timeToLive(int ttl) {
            super.timeToLive(ttl);
            return this;
        }

        @Override
        public PrivateHiddenKeyBuilder timeToBirth(int ttb) {
            super.timeToBirth(ttb);
            return this;
        }

        @Override
        public PrivateHiddenKey build() {
            return (PrivateHiddenKey) _atKey;
        }
    }

}
