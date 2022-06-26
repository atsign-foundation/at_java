package org.atsign.common;

import java.time.OffsetDateTime;

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
            if (dataSignature != null) s += ":dataSignature:" + ccd;
            if (sharedKeyStatus != null) s += ":sharedKeyStatus:" + sharedKeyStatus;
            if (sharedKeyEnc != null) s += ":sharedKeyEnc:" + sharedKeyEnc;
            if (pubKeyCS != null) s += ":pubKeyCS:" + pubKeyCS;
            return s;
        }
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
