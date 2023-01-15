package org.atsign.client.util;

public class KeyStringUtil {

    public enum KeyType {
        PUBLIC_KEY, // PublicKey
        SHARED_KEY, // SharedKey
        SELF_KEY, // SelfKey
        PRIVATE_HIDDEN_KEY, // PrivateHiddenKey
        ;
    }

    private String _fullKeyName; // e.g. "public:publickey@alice"

    private String _keyName; // should never be null (otherwise it's an error)
    private KeyType _keyType; // see enum above, should never be null (otherwise it's an error)
    private String _namespace; // nullable (some keys don't have namespaces)

    private String _sharedBy; // should never be null (all keys have a sharedBy atsign)
    private String _sharedWith; // nullable

    private boolean _isCached; // true if key starts with "cached:"
    private boolean _isHidden; // true if key contains "_"

    /**
     * Constructor
     * @param fullKeyName full key name e.g. "public:publickey@bob"
     */
    public KeyStringUtil(String fullKeyName) {
        this._fullKeyName = fullKeyName;
        this._evaluate(fullKeyName);
    }

    /**
     * Returns the full key name (originally passed into the constructor)
     * @return fullKeyName (what was originally passed into the constructor)
     */
    public String getFullKeyName() {
        return this._fullKeyName;
    }

    /**
     * Returns the key name (e.g. "publickey" from "public:publickey@alice")
     * This value is evaluated from the private _evaluate method that is called in the constructor
     * @return the key name
     */
    public String getKeyName() {
        return this._keyName;
    }
    
    /**
     * Returns the namespace of a key (no implementation yet)
     * @return the namespace from a key (e.g. "mospherepro" from "file_1.mospherepro@alice")
     */
    public String getNamespace() { // no namespace implementation in _evaluate
        return this._namespace;
    }

    /**
     * Returns the key type enum of the key type evaluated from the private _evaluate method
     * @return KeyStringUtil.KeyType (e.g. KeyStringUtil.KeyType.PUBLIC_KEY)
     */
    public KeyType getKeyType() {
        return this._keyType;
    }

    /**
     * Returns the sharedBy atSign that is evlauated from the _evaluate private method.
     * @return the sharedBy atSign String (e.g. "@alice" from "test@alice")
     */
    public String getSharedBy() {
        return this._sharedBy;
    }

    /**
     * Returns the sharedWith atSign that is evlauated from the _evaluate private method.
     * @return the sharedWith atSign String (e.g. "@bob" from "@bob:test@alice")
     */
    public String getSharedWith() {
        return this._sharedWith;
    }
    
    /**
     * Returns true if the key is cached (e.g. "cached:public:publickey@alice")
     * @return true if the fullKeyName begins with "cached:"
     */
    public boolean isCached() {
        return this._isCached;
    }

    /**
     * Returns true if the key is hidden by default in scan
     * @return true if the fullKeyName begins with "_"
     */
    public boolean isHidden() {
        return this._isHidden;
    }

    /**
     * Given the fullKeyName, this method will evaluate all of the properties that can be exactracted from the fullKeyName. Example: fullKeyName "test@bob" will evaluate sharedBy to be "@bob" and keyName to be "test"
     * @param fullKeyName the fullKeyName to be evaluated (e.g. "test@bob")
     */
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
        

        // all keys may have a namespace [uncomment here to add partial namespace support]
        // if(fullKeyName.contains(".")) {
        //     String[] split2 = fullKeyName.split("\\.");
        //     // atconnections.wildgreen.smoothalligator.at_contact.mospherepro@smoothalligator
        //     if(split2.length == 1) {
        //         String[] split3 = split2[1].split("@");
        //         _namespace = split3[0];
        //     }
        // }

        if(split1.length > 1) {
            // must be scenarios 1, 2, 3, 4, 

            // PublicKey check
            if(split1[0].equals("public") || (split1[0].equals("cached") && split1[1].equals("public"))) {
                // scenario 1 and 2,, it's a public key!
                _keyType = KeyType.PUBLIC_KEY;
            } else if(split1[0].equals("private") || split1[0].equals("privatekey")) {
                _keyType = KeyType.PRIVATE_HIDDEN_KEY;
                _isHidden = true;
            }
            
            if(split1[0].startsWith("@") || split1[1].startsWith("@")) {
                // scenario 3 and 4,, it is a SharedKey!
                if(_keyType == null) _keyType = KeyType.SHARED_KEY; // don't want to overwrite the above checks
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

            if(_keyName.startsWith("shared_key")) {
                // SelfKey with _keyName (like `shared_key.bob@alice`) are keys with no namespace
                _namespace = null;
            }
        }

        if(_sharedBy != null) _sharedBy = "@" + _sharedBy; // add atSign in front
        if(_sharedWith != null) _sharedWith = "@" + _sharedWith; // add atSign in front
        if(!_isHidden)  _isHidden = _keyName.startsWith("_"); 
    }

}
