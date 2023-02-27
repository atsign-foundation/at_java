package org.atsign.common;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.atsign.client.api.Secondary;
import org.atsign.client.util.KeyStringUtil;
import org.atsign.client.util.KeyStringUtil.KeyType;
import org.atsign.common.exceptions.MalformedKeyException;

import java.text.ParseException;

@SuppressWarnings("unused")
public abstract class Keys {
    public static AtSign defaultAtSign;

    public static abstract class AtKey {
        public String name;
        public AtSign sharedWith;
        public AtSign sharedBy;
        private String namespace;
        public String getNamespace() { return namespace; }
        public void setNamespace(String namespace) {
            if (namespace != null) {
                while (namespace.startsWith(".")) {
                    namespace = namespace.substring(1);
                }
                namespace = namespace.trim();
            }
            this.namespace = namespace;
        }
        public Metadata metadata;
        // bool isRef = false;
        public AtKey(AtSign sharedBy) {
            if (sharedBy == null) {
                throw new IllegalArgumentException("AtKey: sharedBy may not be null");
            }
            this.sharedBy = sharedBy;
            this.metadata = new Metadata();
        }

        public String getFullyQualifiedKeyName() {
            return name + (namespace != null && ! namespace.trim().isEmpty()
                    ? "." + namespace
                    : "");
        }

        @Override
        public String toString() {
            String s = "";
            if (metadata.isPublic) {
                s += "public:";
            } else if (sharedWith != null) {
                s += sharedWith + ":";
            }
            s += getFullyQualifiedKeyName();

            if (sharedBy != null) {
                s += sharedBy;
            }
            return s;
        }
    }

    /// Represents a public key.
    public static class PublicKey extends AtKey {
        public PublicKey() {
            this(defaultAtSign);
        }
        public PublicKey(AtSign sharedBy) {
            super(sharedBy);
            super.metadata.isPublic = true;
            super.metadata.isEncrypted = false;
            super.metadata.isHidden = false;
        }
    }

    ///Represents a Self key.
    public static class SelfKey extends AtKey {
        public SelfKey() {
            this(defaultAtSign);
        }
        public SelfKey(AtSign sharedBy) {
            this(sharedBy, null);
        }
        public SelfKey(AtSign sharedBy, AtSign sharedWith) { // possibility of `@bob:keyName@bob`
            super(sharedBy);
            super.sharedWith = sharedWith;
            super.metadata.isPublic = false;
            super.metadata.isEncrypted = true;
            super.metadata.isHidden = false;
        }
    }

    /// Represents a key shared to another atSign.
    public static class SharedKey extends AtKey {
        public SharedKey(AtSign sharedBy, AtSign sharedWith) {
            super(sharedBy);
            if (sharedWith == null) {
                throw new IllegalArgumentException("SharedKey: sharedWith may not be null");
            }
            super.sharedWith = sharedWith;
            super.metadata.isPublic = false;
            super.metadata.isEncrypted = true;
            super.metadata.isHidden = false;
        }

        public static SharedKey fromString(String key) throws IllegalArgumentException {
            if (key == null) {
                throw new IllegalArgumentException("SharedKey.fromString(key) : key may not be null");
            }
            String[] splitByColon = key.split(":");
            if (splitByColon.length != 2) {
                throw new IllegalArgumentException("SharedKey.fromString(key) : key must have structure @bob:shared_key@alice");
            }
            String sharedWith = splitByColon[0];
            String[] splitByAtSign = splitByColon[1].split("@");
            if (splitByAtSign.length != 2) {
                throw new IllegalArgumentException("SharedKey.fromString(key) : key must have structure @bob:shared_key@alice");
            }
            String keyName = splitByAtSign[0];
            String sharedBy = splitByAtSign[1];
            SharedKey sharedKey = new SharedKey (new AtSign(sharedBy), new AtSign(sharedWith));
            sharedKey.name = keyName;
            return sharedKey;
        }

        public String getSharedSharedKeyName() {
            return sharedWith + ":shared_key" + sharedBy;
        }
    }

    // Represents a Private hidden key.
    public static class PrivateHiddenKey extends AtKey {
        public PrivateHiddenKey() {
            this(defaultAtSign);
        }
        public PrivateHiddenKey(AtSign sharedBy) {
            super(sharedBy);
            super.metadata = new Metadata();
        }
    }

    /**
     * Generate an AtKey object from a given full key name.
     * @param fullAtKeyName eg: @bob:phone@alice
     * @return AtKey object
     * @throws AtException
     */
    @SuppressWarnings("JavaDoc")
    public static AtKey fromString(String fullAtKeyName) throws AtException {
        KeyStringUtil keyStringUtil = new KeyStringUtil(fullAtKeyName);
        KeyType keyType = keyStringUtil.getKeyType();
        String keyName = keyStringUtil.getKeyName();
        AtSign sharedBy = new AtSign(keyStringUtil.getSharedBy());
        AtSign sharedWith = null;
        if(keyStringUtil.getSharedWith() != null) {
            sharedWith = new AtSign(keyStringUtil.getSharedWith());
        }
        String namespace = keyStringUtil.getNamespace();
        boolean isCached = keyStringUtil.isCached();
        boolean isHidden = keyStringUtil.isHidden();
        AtKey atKey;
        switch(keyType) {
            case PUBLIC_KEY:
                atKey = new KeyBuilders.PublicKeyBuilder(sharedBy).key(keyName).build();
                break;
            case SHARED_KEY:
                atKey = new KeyBuilders.SharedKeyBuilder(sharedBy, sharedWith).key(keyName).build();
                break;
            case SELF_KEY:
                atKey = new KeyBuilders.SelfKeyBuilder(sharedBy, sharedWith).key(keyName).build();
                break;
            case PRIVATE_HIDDEN_KEY:
                atKey = new KeyBuilders.PrivateHiddenKeyBuilder(sharedBy).key(keyName).build();
                break;
            default:
                throw new MalformedKeyException("Could not find KeyType for Key \"" + fullAtKeyName);
        }
        atKey.setNamespace(namespace);
        atKey.metadata.isCached = isCached;
        if(!atKey.metadata.isHidden) atKey.metadata.isHidden = isHidden; // if KeyBuilders constructor did not already evaluate isHidden, then do it here
        return atKey;
    }

    /**
     * Generate an AtKey object whose metadata is populated from the given `llookup:meta:<keyName>` response.
     * @param fullAtKeyName The full AtKey name, eg: `@bob:phone@alice`
     * @param metadataResponse `llookup:meta:<keyName>` rawResponse from secondary server
     * @return AtKey whose metadata is populated from the llookup:meta:<keyName> rawResponse from secondary server
     * @throws AtException
     * @throws ParseException
     */
    @SuppressWarnings("JavaDoc")
    public static AtKey fromString(String fullAtKeyName, Secondary.Response metadataResponse) throws AtException, JsonProcessingException {
        AtKey atKey = fromString(fullAtKeyName);
        atKey.metadata = Metadata.squash(atKey.metadata, Metadata.fromJson(metadataResponse.getRawDataResponse()));
        return atKey;
    }

    public static AtKey fromString(Secondary.Response lookupAllResponse) {
        throw new RuntimeException("Not implemented");
    }
}
