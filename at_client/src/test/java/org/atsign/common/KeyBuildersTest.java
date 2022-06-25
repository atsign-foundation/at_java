package org.atsign.common;


import static org.junit.Assert.assertEquals;

import org.atsign.common.Keys.PublicKey;
import org.atsign.common.Keys.SharedKey;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class KeyBuildersTest {
    
    @Before
    public void setUp() {

    }

    @Test
    public void buildPublicKeyTest() {
        // want to build: 
        String want = "public:publickey@alice"; // this key is public (denoted by `public:` and is sharedBy `@alice` because this atSign is the owner).

        // variables
        String keyName = "publickey";
        String atSignStr = "@alice";
        
        // build PublicKey instance
        AtSign sharedBy = new AtSign(atSignStr);
        PublicKey publicKey = new KeyBuilders.PublicKeyBuilder(sharedBy).key(keyName).build();

        assertEquals(want, publicKey.toString()); // public:publickey@alice
        assertEquals(true, publicKey.metadata.isPublic); // metadata.isPublic
        assertEquals(false, publicKey.metadata.isHidden); // metadata.isHidden
        assertEquals(keyName, publicKey.name); // publickey
        assertEquals(atSignStr, publicKey.sharedBy.atSign); // @alice
        assertEquals(null, publicKey.sharedWith); // null
    }

    @Test
    public void buildSharedKeyTest() {
        // want to build:
        String want = "@bob:shared_key@alice"; // `shared_key` is sharedBy `@alice` and sharedWith `@bob`

        // variables 
        String keyName = "shared_key";
        String sharedByStr = "@alice";
        String sharedWithStr = "@bob";

        // build SharedKey instance
        AtSign sharedBy = new AtSign(sharedByStr);
        AtSign sharedWith = new AtSign(sharedWithStr);

        SharedKey sharedKey = new KeyBuilders.SharedKeyBuilder(sharedBy, sharedWith).key(keyName).build();

        assertEquals(want, sharedKey.toString());
        assertEquals(false, sharedKey.metadata.isPublic);
    }

    @After
    public void tearDown() {

    }

}
