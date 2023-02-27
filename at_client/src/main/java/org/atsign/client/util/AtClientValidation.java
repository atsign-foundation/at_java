package org.atsign.client.util;

import org.atsign.client.api.Secondary;
import org.atsign.common.AtSign;
import org.atsign.common.Keys.AtKey;
import org.atsign.common.Metadata;
import org.atsign.common.AtException;
import org.atsign.common.exceptions.AtIllegalArgumentException;
import org.atsign.common.exceptions.AtInvalidAtKeyException;
import org.atsign.common.exceptions.AtSecondaryConnectException;

import java.io.IOException;

public class AtClientValidation {
    // TODO Lots of atServer-address-finding going on here. Need a caching finder.
    // TODO Change all of these static methods.
    /**
     * @param keyName // e.g. "test" (not the fullKeyName like "public:test@bob")
     */
    public static void validateKeyName(String keyName) throws AtException {
        // key cannot be null and cannot be the empty string (length 0)
        if (keyName == null || keyName.isEmpty()) {
            throw new AtInvalidAtKeyException("Key cannot be null or empty");
        }

        // key cannot have spaces
        if (keyName.contains(" ")) {
            throw new AtInvalidAtKeyException("Key cannot have spaces");
        }

        // Key cannot contain @
        if (keyName.contains("@")) {
            throw new AtInvalidAtKeyException("Key cannot contain @");
        }
    }

    /**
     * 
     * @param metadata Metadata object to validate
     * @throws AtException if metadata is null or has invalid values
     */
    public static void validateMetadata(Metadata metadata) throws AtException {
        // null check
        if (metadata == null) {
            throw new AtInvalidAtKeyException("Metadata cannot be null");
        }

        // validate ttl
        if (metadata.ttl == null || (metadata.ttl < 0)) {
            throw new AtInvalidAtKeyException("ttl cannot be null and cannot be negative");
        }

        // validate ttb
        if (metadata.ttb == null || metadata.ttb < 0) {
            throw new AtInvalidAtKeyException("ttb cannot be null and cannot be negative");
        }

        // validate ttr
        if (metadata.ttr == null || metadata.ttr < -1) {
            throw new AtInvalidAtKeyException("ttb cannot be null and cannot be < -1");
        }

    }

    /**
     * Checks if an atSign exists on a root server
     * 
     * @param atSign  atSign object to check for existence
     * @param rootUrl rootUrl of the root server for atSign existence
     * @throws AtException if atSign does not exist/address could not be found, if
     *                     atSign object is empty,
     */
    public static void atSignExists(AtSign atSign, String rootUrl) throws AtException {
        if(atSign == null || atSign.toString().isEmpty()) {
            throw new AtIllegalArgumentException("atSign cannot be null or empty");
        }
        if (rootUrl == null || rootUrl.isEmpty()) {
            throw new AtIllegalArgumentException("rootUrl cannot be null or empty");
        }
        Secondary.AddressFinder finder = ArgsUtil.createAddressFinder(rootUrl);
        try {
        finder.findSecondary(atSign);
        } catch (IOException e) {
            throw new AtSecondaryConnectException("Exception while trying to find secondary of atSign: " + atSign, e);
        }
    }

    /**
     * Validate if an AtKey object is valid
     * 1. checks if atKey.name is a valid keyName
     * 2. checks if atKey.metadata has valid values
     * 3. checks if atKey.sharedWith atSign is valid (if it exists)
     * 
     * @param atKey   AtKey object created usually through KeyBuilders
     * @param rootUrl RootURL of the Root Server (e.g. "root.atsign.org:64")
     * @throws AtException if the AtKey is invalid
     */
    public static void validateAtKey(AtKey atKey, String rootUrl) throws AtException {
        // 1. null check
        if(atKey == null) {
            throw new AtIllegalArgumentException("AtKey cannot be null");
        }

        if(rootUrl == null || rootUrl.isEmpty()) {
            throw new AtIllegalArgumentException("RootURL cannot be null or empty");
        }

        // 2. validate key name
        validateKeyName(atKey.name);

        // 3. validate metadata
        validateMetadata(atKey.metadata);

        // 4. validate sharedWith exists
        if (atKey.sharedWith != null) {
            atSignExists(atKey.sharedWith, rootUrl);
        }

    }

}
