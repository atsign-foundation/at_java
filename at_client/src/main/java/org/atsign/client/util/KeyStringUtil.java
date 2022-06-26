package org.atsign.client.util;

import org.atsign.common.AtSign;

public class KeyStringUtil {

    public enum KeyType {
        PUBLIC_KEY, // PublicKey
        SHARED_KEY, // SharedKey
        SELF_KEY, // SelfKey
        PRIVATE_HIDDEN_KEY, // PrivateHiddenKey
        ;
    }

    private String _keyName; // should never be null (otherwise it's an error)
    private KeyType _keyType; // see enum above, should never be null (otherwise it's an error)
    private String _namespace; // nullable (some keys don't have namespaces)

    private String _sharedBy; // should never be null (all keys have a sharedBy atsign)
    private String _sharedWith; // nullable

    private boolean _isCached; // true if key starts with "cached:"

    public KeyStringUtil(String fullKeyName) {
        this._evaluate(fullKeyName);
    }

    public String getKeyName() {
        return this._keyName;
    }
    
    public String getNamespace() {
        return this._namespace;
    }

    public KeyType getKeyType() {
        return this._keyType;
    }

    public String getSharedBy() {
        return this._sharedBy;
    }

    public String getSharedWith() {
        return this._sharedWith;
    }
    
    public boolean isCached() {
        return this._isCached;
    }

    private void _evaluate(String fullKeyName) {
        // Examples:
        // (1) PublicKey                == public:signing_publickey@smoothalligator
        // (2) PublicKey (cached)       == cached:public:publickey@denise
        // (3) SharedKey                == @abbcservicesinc:shared_key@smoothalligator
        // (4) SharedKey (cached)       == cached:@smoothalligator:shared_key@abbcservicesinc
        // (5) PrivateHiddenKey         == _latestnotificationid.fourballcorporate9@smoothalligator
        // (6) SelfKey                  == shared_key.wildgreen@smoothalligator
        // (7) SelfKey                  == @smoothalligator:lemon@smoothalligator
        String[] split1 = fullKeyName.split(":");
        // split1 results
        // 1 == {"public", "signing_publickey@smoothalligator"} [len 2]
        // 2 == {"cached", "public", "publickey@denise"} [len 3]
        // 3 == {"@abbcservicesinc", "shared_key@smoothalligator"} [len 2]
        // 4 == {"cached", "@smoothalligator", "shared_key@abbcservicesinc"} [len 3]
        // 5 == {"_latestnotificationid.fourballcorporate9@smoothalligator"} [len 1]
        // 6 == {"shared_key.wildgreen@smoothalligator"} [len 1]
        

        // all keys may have a namespace
        if(fullKeyName.contains(".")) {
            String[] split2 = fullKeyName.split("\\.");
            if(split2.length > 1) {
                String[] split3 = split2[1].split("@");
                _namespace = split3[0];
            }
        }

        if(split1.length > 1) {
            // must be scenarios 1, 2, 3, 4, 

            // PublicKey check
            if(split1[0].equals("public") || (split1[0].equals("cached") && split1[1].equals("public"))) {
                // scenario 1 and 2,, it's a public key!
                _keyType = KeyType.PUBLIC_KEY;
            }

            // SharedKey check
            if(split1[0].startsWith("@") || split1[1].startsWith("@")) {
                // scenario 3 and 4,, it is a SharedKey!
                _keyType = KeyType.SHARED_KEY;
                if(split1[0].startsWith("@")) {
                    _sharedWith = split1[0].substring(1);
                } else {
                    _sharedWith = split1[1].substring(1);
                }
            }

            String[] split2 = split1[split1.length-1].split("@");
            // 1 == {"signing_publickey", "smoothalligator"}
            // 2 == {"publickey", "denise"}
            // 3 == {"shared_key", "smoothalligator"}
            // 4 == {"shared_key", "abbcservicesinc"}
            _keyName = split2[0];
            _sharedBy = split2[1];

            // PublicKey and SharedKey can be cacheable!
            if(split1[0].equals("cached")) {
                _isCached = true;
            }

            // _sharedBy == _sharedWith => it's a SelfKey
            if(_sharedBy.equals(_sharedWith)) {
                _keyType = KeyType.SELF_KEY;
            }

        } else {
            // must be scenarios 5 and 6
            if(split1[0].startsWith("_")) {
                _keyType = KeyType.PRIVATE_HIDDEN_KEY;
            } else {
                _keyType = KeyType.SELF_KEY;
            }

            String[] split2 = split1[0].split("@");
            // 5 == {"_latestnotificationid.fourballcorporate9", "smoothalligator"}
            // 6 == {"shared_key.wildgreen", "smoothalligator"}
            _keyName = split2[0];
            _sharedBy = split2[1];
        }
    }

}
