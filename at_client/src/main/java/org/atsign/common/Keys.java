package org.atsign.common;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.Date;
import java.util.Map;
import java.util.TimeZone;

import org.atsign.client.api.Secondary;
import org.atsign.client.util.DateUtil;
import org.atsign.client.util.KeyStringUtil;
import org.atsign.client.util.KeyStringUtil.KeyType;
import org.atsign.common.ResponseTransformers.LlookupMetadataResponseTransformer;

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
            if(metadata.isCached) {
                s += "cached:";
            }
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

    public static class Metadata {
        public Integer ttl;
        public Integer ttb;
        public Integer ttr;
        public Boolean ccd;
        public OffsetDateTime availableAt;
        public OffsetDateTime expiresAt;
        public OffsetDateTime refreshAt;
        public OffsetDateTime createdAt;
        public OffsetDateTime updatedAt;
        public String dataSignature;
        public String sharedKeyStatus;
        public Boolean isPublic = false;
        public Boolean isEncrypted = true;
        public Boolean isHidden = false;
        public Boolean namespaceAware = true;
        public Boolean isBinary = false;
        public Boolean isCached = false;
        public String sharedKeyEnc;
        public String pubKeyCS;

        @Override
        public String toString() {
            String s = "";
            if (ttl != null) s += ":ttl:" + ttl;
            if (ttb != null) s += ":ttb:" + ttb;
            if (ttr != null) s += ":ttr:" + ttr;
            if (ccd != null) s += ":ccd:" + ccd;
            if (dataSignature != null) s += ":dataSignature:" + dataSignature;
            if (sharedKeyStatus != null) s += ":sharedKeyStatus:" + sharedKeyStatus;
            if (sharedKeyEnc != null) s += ":sharedKeyEnc:" + sharedKeyEnc;
            if (pubKeyCS != null) s += ":pubKeyCS:" + pubKeyCS;
            if (isBinary != null) s += ":isBinary:" + isBinary;
            if (isEncrypted != null) s += ":isEncrypted:" + isEncrypted;
            return s;
        }

        /**
         * Create metadata object from a Response
         * @param rawLlookupMetaResponse Secondary.Response from executingVerb `llookup:meta:<keyName>`
         * @return Metadata object
         * @throws ParseException if dates from metadata llookup could not be parsed
         */
        public static Metadata fromString(Secondary.Response rawLlookupMetaResponse) throws ParseException {
            Metadata metadata = new Metadata();
            LlookupMetadataResponseTransformer transformer = new LlookupMetadataResponseTransformer();
            Map<String, Object> map = transformer.transform(rawLlookupMetaResponse);
            metadata.ttl = (Integer) map.get("ttl");
            metadata.ttb = (Integer) map.get("ttb");
            metadata.ttr = (Integer) map.get("ttr");
            metadata.ccd = (Boolean) map.get("ccd");        
            metadata.availableAt = DateUtil.parse((String) map.get("availableAt"));
            metadata.expiresAt = DateUtil.parse((String) map.get("expiresAt"));
            metadata.refreshAt = DateUtil.parse((String) map.get("refreshAt"));
            metadata.createdAt = DateUtil.parse((String) map.get("createdAt"));
            metadata.updatedAt = DateUtil.parse((String) map.get("updatedAt"));
            metadata.isBinary = (Boolean) map.get("isBinary");
            metadata.isEncrypted = (Boolean) map.get("isEncrypted");
            metadata.dataSignature = (String) map.get("dataSignature");
            metadata.sharedKeyEnc = (String) map.get("sharedKeyEnc");
            metadata.pubKeyCS = (String) map.get("pubKeyCS");
            return metadata;
        }

        /**
         * Squashes the two metadatas into one metadata.
         * @param atKeyMetadata Metadata from atKey.metadata (has priority)
         * @param metadataUtilMetadata Metadata from MetadataUtil.java
         * @return One merged metadata object
         *
         */
        public static Metadata squash(Metadata atKeyMetadata, Metadata metadataUtilMetadata) {
            Metadata metadata = new Metadata();
            if(atKeyMetadata.ttl != null) metadata.ttl = atKeyMetadata.ttl;
            else if(metadataUtilMetadata.ttl != null) metadata.ttl = metadataUtilMetadata.ttl;

            if(atKeyMetadata.ttb != null) metadata.ttb = atKeyMetadata.ttb;
            else if(metadataUtilMetadata.ttb != null) metadata.ttb = metadataUtilMetadata.ttb;

            if(atKeyMetadata.ttr != null) metadata.ttr = atKeyMetadata.ttr;
            else if(metadataUtilMetadata.ttr != null) metadata.ttr = metadataUtilMetadata.ttr;

            if(atKeyMetadata.ccd != null) metadata.ccd = atKeyMetadata.ccd;
            else if(metadataUtilMetadata.ccd != null) metadata.ccd = metadataUtilMetadata.ccd;

            if(atKeyMetadata.availableAt != null) metadata.availableAt = atKeyMetadata.availableAt;
            else if(metadataUtilMetadata.availableAt != null) metadata.availableAt = metadataUtilMetadata.availableAt;

            if(atKeyMetadata.expiresAt != null) metadata.expiresAt = atKeyMetadata.expiresAt;
            else if(metadataUtilMetadata.expiresAt != null) metadata.expiresAt = metadataUtilMetadata.expiresAt;

            if(atKeyMetadata.refreshAt != null) metadata.refreshAt = atKeyMetadata.refreshAt;
            else if(metadataUtilMetadata.refreshAt != null) metadata.refreshAt = metadataUtilMetadata.refreshAt;

            if(atKeyMetadata.createdAt != null) metadata.createdAt = atKeyMetadata.createdAt;
            else if(metadataUtilMetadata.createdAt != null) metadata.createdAt = metadataUtilMetadata.createdAt;

            if(atKeyMetadata.updatedAt != null) metadata.updatedAt = atKeyMetadata.updatedAt;
            else if(metadataUtilMetadata.updatedAt != null) metadata.updatedAt = metadataUtilMetadata.updatedAt;            

            if(atKeyMetadata.dataSignature != null) metadata.dataSignature = atKeyMetadata.dataSignature;
            else if(metadataUtilMetadata.dataSignature != null) metadata.dataSignature = metadataUtilMetadata.dataSignature;

            if(atKeyMetadata.sharedKeyStatus != null) metadata.sharedKeyStatus = atKeyMetadata.sharedKeyStatus;
            else if(metadataUtilMetadata.sharedKeyStatus != null) metadata.sharedKeyStatus = metadataUtilMetadata.sharedKeyStatus;

            if(atKeyMetadata.sharedKeyEnc != null) metadata.sharedKeyEnc = atKeyMetadata.sharedKeyEnc;
            else if(metadataUtilMetadata.sharedKeyEnc != null) metadata.sharedKeyEnc = metadataUtilMetadata.sharedKeyEnc;

            if(atKeyMetadata.isPublic != null) metadata.isPublic = atKeyMetadata.isPublic;
            else if(metadataUtilMetadata.isPublic != null) metadata.isPublic = metadataUtilMetadata.isPublic;

            if(atKeyMetadata.isEncrypted != null) metadata.isEncrypted = atKeyMetadata.isEncrypted;
            else if(metadataUtilMetadata.isEncrypted != null) metadata.isEncrypted = metadataUtilMetadata.isEncrypted;

            if(atKeyMetadata.isHidden != null) metadata.isHidden = atKeyMetadata.isHidden;
            else if(metadataUtilMetadata.isHidden != null) metadata.isHidden = metadataUtilMetadata.isHidden;

            if(atKeyMetadata.namespaceAware != null) metadata.namespaceAware = atKeyMetadata.namespaceAware;
            else if(metadataUtilMetadata.namespaceAware != null) metadata.namespaceAware = metadataUtilMetadata.namespaceAware;

            if(atKeyMetadata.isBinary != null) metadata.isBinary = atKeyMetadata.isBinary;
            else if(metadataUtilMetadata.isBinary != null) metadata.isBinary = metadataUtilMetadata.isBinary;

            if(atKeyMetadata.isCached != null) metadata.isCached = atKeyMetadata.isCached;
            else if(metadataUtilMetadata.isCached != null) metadata.isCached = metadataUtilMetadata.isCached;

            if(atKeyMetadata.sharedKeyEnc != null) metadata.sharedKeyEnc = atKeyMetadata.sharedKeyEnc;
            else if(metadataUtilMetadata.sharedKeyEnc != null) metadata.sharedKeyEnc = metadataUtilMetadata.sharedKeyEnc;

            if(atKeyMetadata.pubKeyCS != null) metadata.pubKeyCS = atKeyMetadata.pubKeyCS;
            else if(metadataUtilMetadata.pubKeyCS != null) metadata.pubKeyCS = metadataUtilMetadata.pubKeyCS;

            return metadata;
        }
    }

    /**
     * Generate an AtKey object from a given full key name.
     * @param fullAtKeyName eg: @bob:phone@alice
     * @return AtKey object
     * @throws AtException
     */
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
        AtKey atKey = null;
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
                throw new AtException("Key \"" + fullAtKeyName + "\" was not given a KeyType");
        }
        atKey.setNamespace(namespace);
        atKey.metadata.isCached = isCached;
        if(!atKey.metadata.isHidden) atKey.metadata.isHidden = isHidden; // if KeyBuilders constructor did not already evaluate isHidden, then do it here
        return atKey;
    }

    /**
     * Generate an AtKey object whose metadata is populated from the given `llookup:meta:<keyName>` response.
     * @param fullAtKeyName The full AtKey name, eg: `@bob:phone@alice`
     * @param llookedUpMetadata `llookup:meta:<keyName>` rawResponse from secondary server
     * @return AtKey whose metadata is populated from the llookup:meta:<keyName> rawResponse from secondary server
     * @throws AtException
     */
    public static AtKey fromString(String fullAtKeyName, Secondary.Response llookedUpMetadata) throws AtException, ParseException {
        AtKey atKey = fromString(fullAtKeyName);
        atKey.metadata = Metadata.squash(atKey.metadata, Metadata.fromString(llookedUpMetadata));
        return atKey;
    }
}

//        static AtKey fromString(String key) {
//            var atKey = AtKey();
//            var metaData = Metadata();
//            if (key.startsWith(AT_PKAM_PRIVATE_KEY) ||
//                    key.startsWith(AT_PKAM_PUBLIC_KEY)) {
//                atKey.key = key;
//                atKey.metadata = metaData;
//                return atKey;
//            } else if (key.startsWith(AT_ENCRYPTION_PRIVATE_KEY)) {
//                atKey.key = key.split('@')[0];
//                atKey.sharedBy = key.split('@')[1];
//                atKey.metadata = metaData;
//                return atKey;
//            }
//            //If key does not contain '@'. or key has space, it is not a valid key.
//            if (!key.contains('@') || key.contains(' ')) {
//                throw InvalidSyntaxException('$key is not well-formed key');
//            }
//            var keyParts = key.split(':');
//            // If key does not contain ':' Ex: phone@bob; then keyParts length is 1
//            // where phone is key and @bob is sharedBy
//            if (keyParts.length == 1) {
//                atKey.sharedBy = keyParts[0].split('@')[1];
//                atKey.key = keyParts[0].split('@')[0];
//            } else {
//                // Example key: public:phone@bob
//                if (keyParts[0] == 'public') {
//                    metaData.isPublic = true;
//                }
//                // Example key: cached:@alice:phone@bob
//                else if (keyParts[0] == CACHED) {
//                    metaData.isCached = true;
//                    atKey.sharedWith = keyParts[1];
//                } else {
//                    atKey.sharedWith = keyParts[0];
//                }
//                List<String> keyArr = [];
//                if (keyParts[0] == CACHED) {
//                    keyArr = keyParts[2].split('@');
//                } else {
//                    keyArr = keyParts[1].split('@');
//                }
//                if (keyArr.length == 2) {
//                    atKey.sharedBy = keyArr[1];
//                    atKey.key = keyArr[0];
//                } else {
//                    atKey.key = keyArr[0];
//                }
//            }
//            //remove namespace
//            if (atKey.key != null && atKey.key !.contains('.')){
//                var namespaceIndex = atKey.key !.lastIndexOf('.');
//                if (namespaceIndex > -1) {
//                    atKey.namespace = atKey.key !.substring(namespaceIndex + 1);
//                    atKey.key = atKey.key !.substring(0, namespaceIndex);
//                }
//            } else{
//                metaData.namespaceAware = false;
//            }
//            atKey.metadata = metaData;
//            return atKey;
//        }
