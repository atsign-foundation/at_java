package org.atsign.common;

import static org.junit.Assert.assertThrows;

import org.atsign.client.util.AtClientValidation;
import org.atsign.common.Keys.PublicKey;
import org.atsign.common.Keys.SelfKey;
import org.atsign.common.Keys.SharedKey;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class AtClientValidationTest {

    @Before
    public void setUp() {
    }

    // valid key names (like "test", but not "cached:public:test@bob" <-- these key
    // names will always fail this test)
    @Test
    public void validateKeyNameTest() {
        // null key name
        assertThrows(AtException.class, () -> {
            String keyName = null;
            AtClientValidation.validateKeyName(keyName);
        });

        // empty key name
        assertThrows(AtException.class, () -> {
            String keyName = "";
            AtClientValidation.validateKeyName(keyName);
        });

        // key name with @
        assertThrows(AtException.class, () -> {
            String keyName = "test@bob";
            AtClientValidation.validateKeyName(keyName);
        });

        // key name with spaces
        assertThrows(AtException.class, () -> {
            String keyName = " te st ";
            AtClientValidation.validateKeyName(keyName);
        });

    }

    // valid metadata (null check, ttl, ttb, ttr)
    @Test
    public void validateMetadataTest() {

        // null metadata
        assertThrows(AtException.class, () -> {
            Metadata metadata = null;
            AtClientValidation.validateMetadata(metadata);
        });

        // null ttl
        assertThrows(AtException.class, () -> {
            Metadata metadata = new Metadata();
            metadata.ttl = null;
            metadata.ttb = 0;
            metadata.ttr = -1;
            AtClientValidation.validateMetadata(metadata);
        });

        // negative ttl
        assertThrows(AtException.class, () -> {
            Metadata metadata = new Metadata();
            metadata.ttl = -100;
            metadata.ttb = 0;
            metadata.ttr = -1;
            AtClientValidation.validateMetadata(metadata);
        });

        // null ttb
        assertThrows(AtException.class, () -> {
            Metadata metadata = new Metadata();
            metadata.ttl = 0;
            metadata.ttb = null;
            metadata.ttr = -1;
            AtClientValidation.validateMetadata(metadata);
        });

        // negative ttb
        assertThrows(AtException.class, () -> {
            Metadata metadata = new Metadata();
            metadata.ttl = 0;
            metadata.ttb = -100;
            metadata.ttr = -1;
            AtClientValidation.validateMetadata(metadata);
        });

        // null ttr
        assertThrows(AtException.class, () -> {
            Metadata metadata = new Metadata();
            metadata.ttl = 0;
            metadata.ttb = 0;
            metadata.ttr = null;
            AtClientValidation.validateMetadata(metadata);
        });

        // ttr < -1
        assertThrows(AtException.class, () -> {
            Metadata metadata = new Metadata();
            metadata.ttl = 0;
            metadata.ttb = 0;
            metadata.ttr = -2;
            AtClientValidation.validateMetadata(metadata);
        });
    }

    // atSign exists (uses secondaryaddress.finder)
    @Test
    public void atSignExistsTest() {

        final String VALID_ROOT_URL = "root.atsign.org:64"; // prod rootUrl

        // null atSign
        assertThrows(AtException.class, () -> {
            AtSign atSign = null;
            AtClientValidation.atSignExists(atSign, VALID_ROOT_URL);
        });

        // empty atSign
        assertThrows(IllegalArgumentException.class, () -> {
            AtSign atSign = new AtSign("");
            AtClientValidation.atSignExists(atSign, VALID_ROOT_URL);
        });

        // root does not contain atSign
        assertThrows(AtException.class, () -> {
            AtSign atSign = new AtSign("someAtSignThatDNE456");
            AtClientValidation.atSignExists(atSign, VALID_ROOT_URL);
        });

        // invalid root
        assertThrows(AtException.class, () -> {
            AtSign atSign = new AtSign("smoothalligator");
            AtClientValidation.atSignExists(atSign, "invalidroot:32123");
        });

    }

    // validate AtKey object is ready (checks atKey.name validity, metadata
    // validity, and if sharedWith exists)
    @Test
    public void validateAtKeyTest() {
        final String ROOT_URL = "root.atsign.org:64";

        // ====================================
        // PublicKey tests
        // ====================================

        // null publicKey
        assertThrows(AtException.class, () -> {
            PublicKey publicKey = null;
            AtClientValidation.validateAtKey(publicKey, ROOT_URL);
        });

        // public key with invalid ttr
        assertThrows(AtException.class, () -> {
            AtSign sharedBy = new AtSign("@bob");
            PublicKey publicKey = new KeyBuilders.PublicKeyBuilder(sharedBy).key("test").build();
            publicKey.metadata.ttr = -2;
            AtClientValidation.validateAtKey(publicKey, "");
        });

        // ====================================
        // SelfKey tests
        // ====================================

        // null selfKey
        assertThrows(AtException.class, () -> {
            SelfKey selfKey = null;
            AtClientValidation.validateAtKey(selfKey, ROOT_URL);
        });

        // self key with invalid keyName
        assertThrows(AtException.class, () -> {
            AtSign sharedBy = new AtSign("@bob");
            SelfKey selfKey = new KeyBuilders.SelfKeyBuilder(sharedBy).key("t est").build();
            AtClientValidation.validateAtKey(selfKey, ROOT_URL);
        });

        // self key with non-existent sharedWith atSign in root
        assertThrows(AtException.class, () -> {
            AtSign sharedBy = new AtSign("@nonexistentatsign");
            AtSign sharedWith = new AtSign("@nonexistentatsign"); // atSign does not exist in root
            SelfKey selfKey = new KeyBuilders.SelfKeyBuilder(sharedBy, sharedWith).key("test").build();
            AtClientValidation.validateAtKey(selfKey, ROOT_URL);
        });

        // ====================================
        // SharedKey tests
        // ====================================

        // null shared key test
        assertThrows(AtException.class, () -> {
            SharedKey sharedKey = null;
            AtClientValidation.validateAtKey(sharedKey, ROOT_URL);
        });

        // shared key with ttr < -1
        assertThrows(AtException.class, () -> {
            AtSign sharedBy = new AtSign("@bob");
            AtSign sharedWith = new AtSign("@alice");
            SharedKey sharedKey = new KeyBuilders.SharedKeyBuilder(sharedBy, sharedWith).key("test").build();
            sharedKey.metadata.ttr = -22323;
            AtClientValidation.validateAtKey(sharedKey, ROOT_URL);
        });

        // shared key with invalid keyName
        assertThrows(AtException.class, () -> {
            AtSign sharedBy = new AtSign("@bob");
            AtSign sharedWith = new AtSign("@alice");
            SharedKey sharedKey = new KeyBuilders.SharedKeyBuilder(sharedBy, sharedWith).key("t est").build();
            AtClientValidation.validateAtKey(sharedKey, ROOT_URL);
        });

        // shared key with invalid sharedWith atSign (atSign dne in root)
        assertThrows(AtException.class, () -> {
            AtSign sharedBy = new AtSign("@wildgreen");
            AtSign sharedWith = new AtSign("@nonexistentatsign"); // atSign does not exist in root
            SharedKey sharedKey = new KeyBuilders.SharedKeyBuilder(sharedBy, sharedWith).key("test").build();
            AtClientValidation.validateAtKey(sharedKey, ROOT_URL);
        });

        // empty root url
        assertThrows(AtException.class, () -> {
            AtSign sharedBy = new AtSign("@bob");
            AtSign sharedWith = new AtSign("@alice");
            SharedKey sharedKey = new KeyBuilders.SharedKeyBuilder(sharedBy, sharedWith).key("test").build();
            AtClientValidation.validateAtKey(sharedKey, "");
        });

        // ====================================
        // PrivateHiddenKey tests
        // ====================================

        // TODO
    }

    @After
    public void tearDown() {
    }

}
