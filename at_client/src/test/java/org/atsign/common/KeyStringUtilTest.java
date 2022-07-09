package org.atsign.common;

import static org.junit.Assert.assertEquals;

import org.atsign.client.util.KeyStringUtil;
import org.atsign.client.util.KeyStringUtil.KeyType;
import org.junit.Test;

public class KeyStringUtilTest {
    // https://docs.google.com/spreadsheets/d/1EOcF_vznBoKWXxRT8dbeG47RP7wnF30ubwXghganTlk/edit#gid=0
    
    // Row 4 Public key
    @Test
    public void publicKey() {
        String KEY_NAME = "public:phone@bob";
        KeyStringUtil keyStringUtil = new KeyStringUtil(KEY_NAME);
        assertEquals("public:phone@bob", keyStringUtil.getFullKeyName());
        assertEquals("phone", keyStringUtil.getKeyName());
        assertEquals(null, keyStringUtil.getNamespace());
        assertEquals(KeyType.PUBLIC_KEY, keyStringUtil.getKeyType());
        assertEquals("@bob", keyStringUtil.getSharedBy());
        assertEquals(null, keyStringUtil.getSharedWith());
        assertEquals(false, keyStringUtil.isCached());
        assertEquals(false, keyStringUtil.isHidden());
    }

    // Row 5 Public Hidden  key
    @Test
    public void publicKey2() {
        String KEY_NAME = "public:_phone@bob";
        KeyStringUtil keyStringUtil = new KeyStringUtil(KEY_NAME);
        assertEquals("public:_phone@bob", keyStringUtil.getFullKeyName());
        assertEquals("_phone", keyStringUtil.getKeyName());
        assertEquals(null, keyStringUtil.getNamespace());
        assertEquals(KeyType.PUBLIC_KEY, keyStringUtil.getKeyType());
        assertEquals("@bob", keyStringUtil.getSharedBy());
        assertEquals(null, keyStringUtil.getSharedWith());
        assertEquals(false, keyStringUtil.isCached());
        assertEquals(true, keyStringUtil.isHidden());
    }

    // Row 6 Public Hidden key
    @Test
    public void publicKey3() {
        String KEY_NAME = "public:__phone@bob";
        KeyStringUtil keyStringUtil = new KeyStringUtil(KEY_NAME);
        assertEquals("public:__phone@bob", keyStringUtil.getFullKeyName());
        assertEquals("__phone", keyStringUtil.getKeyName());
        assertEquals(null, keyStringUtil.getNamespace());
        assertEquals(KeyType.PUBLIC_KEY, keyStringUtil.getKeyType());
        assertEquals("@bob", keyStringUtil.getSharedBy());
        assertEquals(null, keyStringUtil.getSharedWith());
        assertEquals(false, keyStringUtil.isCached());
        assertEquals(true, keyStringUtil.isHidden());
    }

    // Row 7A Public key and SharedWith populated
    public void publicKey4A() {
        String KEY_NAME = "public:@bob:phone@bob";
        KeyStringUtil keyStringUtil = new KeyStringUtil(KEY_NAME);
        assertEquals("public:@bob:phone@bob", keyStringUtil.getFullKeyName());
        assertEquals("phone", keyStringUtil.getKeyName());
        assertEquals(null, keyStringUtil.getNamespace());
        assertEquals(KeyType.PUBLIC_KEY, keyStringUtil.getKeyType());
        assertEquals("@bob", keyStringUtil.getSharedBy());
        assertEquals("@bob", keyStringUtil.getSharedWith());
        assertEquals(false, keyStringUtil.isCached());
        assertEquals(false, keyStringUtil.isHidden());
    }

    // Row 7B Public key and SharedWith populated
    @Test
    public void publicKey4B() {
        String KEY_NAME = "public:@alice:phone@bob";
        KeyStringUtil keyStringUtil = new KeyStringUtil(KEY_NAME);
        assertEquals("public:@alice:phone@bob", keyStringUtil.getFullKeyName());
        assertEquals("phone", keyStringUtil.getKeyName());
        assertEquals(null, keyStringUtil.getNamespace());
        assertEquals(KeyType.PUBLIC_KEY, keyStringUtil.getKeyType());
        assertEquals("@bob", keyStringUtil.getSharedBy());
        assertEquals("@alice", keyStringUtil.getSharedWith());
        assertEquals(false, keyStringUtil.isCached());
    }

    // Row 8 self key (sharedWith not populated)
    @Test
    public void selfKey1() {
        String KEY_NAME = "phone@bob";
        KeyStringUtil keyStringUtil = new KeyStringUtil(KEY_NAME);
        assertEquals("phone@bob", keyStringUtil.getFullKeyName());
        assertEquals("phone", keyStringUtil.getKeyName());
        assertEquals(null, keyStringUtil.getNamespace());
        assertEquals(KeyType.SELF_KEY, keyStringUtil.getKeyType());
        assertEquals("@bob", keyStringUtil.getSharedBy());
        assertEquals(null, keyStringUtil.getSharedWith());
        assertEquals(false, keyStringUtil.isCached());
        assertEquals(false, keyStringUtil.isHidden());
    }

    // Row 9 Self key (sharedWith populated)
    @Test
    public void selfKey2() {
        String KEY_NAME = "@bob:phone@bob";
        KeyStringUtil keyStringUtil = new KeyStringUtil(KEY_NAME);
        assertEquals("@bob:phone@bob", keyStringUtil.getFullKeyName());
        assertEquals("phone", keyStringUtil.getKeyName());
        assertEquals(null, keyStringUtil.getNamespace());
        assertEquals(KeyType.SELF_KEY, keyStringUtil.getKeyType());
        assertEquals("@bob", keyStringUtil.getSharedBy());
        assertEquals("@bob", keyStringUtil.getSharedWith());
        assertEquals(false, keyStringUtil.isCached());
        assertEquals(false, keyStringUtil.isHidden());
    }

    // Row 10 Self hidden key (sharedWith populated)
    @Test
    public void selfKey3() {
        String KEY_NAME = "@bob:_phone@bob";
        KeyStringUtil keyStringUtil = new KeyStringUtil(KEY_NAME);
        assertEquals("@bob:_phone@bob", keyStringUtil.getFullKeyName());
        assertEquals("_phone", keyStringUtil.getKeyName());
        assertEquals(null, keyStringUtil.getNamespace());
        assertEquals(KeyType.SELF_KEY, keyStringUtil.getKeyType());
        assertEquals("@bob", keyStringUtil.getSharedBy());
        assertEquals("@bob", keyStringUtil.getSharedWith());
        assertEquals(false, keyStringUtil.isCached());
        assertEquals(true, keyStringUtil.isHidden());
    }

    // row 11 Self Hidden Key without sharedWith
    @Test
    public void selfKey4() {
        String KEY_NAME = "_phone@bob";
        KeyStringUtil keyStringUtil = new KeyStringUtil(KEY_NAME);
        assertEquals("_phone@bob", keyStringUtil.getFullKeyName());
        assertEquals("_phone", keyStringUtil.getKeyName());
        assertEquals(null, keyStringUtil.getNamespace());
        assertEquals(KeyType.PRIVATE_HIDDEN_KEY, keyStringUtil.getKeyType());
        assertEquals("@bob", keyStringUtil.getSharedBy());
        assertEquals(null, keyStringUtil.getSharedWith());
        assertEquals(false, keyStringUtil.isCached());
        assertEquals(true, keyStringUtil.isHidden());
    }

    // Row 12 SharedKey
    @Test
    public void sharedKey1() {
        String KEY_NAME = "@bob:phone@alice";
        KeyStringUtil keyStringUtil = new KeyStringUtil(KEY_NAME);
        assertEquals("@bob:phone@alice", keyStringUtil.getFullKeyName()); 
        assertEquals("phone", keyStringUtil.getKeyName());
        assertEquals(null, keyStringUtil.getNamespace());
        assertEquals(KeyType.SHARED_KEY, keyStringUtil.getKeyType());  
        assertEquals("@bob", keyStringUtil.getSharedWith());
        assertEquals("@alice", keyStringUtil.getSharedBy());
        assertEquals(false, keyStringUtil.isCached());
        assertEquals(false, keyStringUtil.isHidden());
    }

    // Row 13 Shared and hidden
    @Test
    public void sharedKey2() {
        String KEY_NAME = "@alice:_phone@bob";
        KeyStringUtil keyStringUtil = new KeyStringUtil(KEY_NAME);
        assertEquals("@alice:_phone@bob", keyStringUtil.getFullKeyName());
        assertEquals("_phone", keyStringUtil.getKeyName());
        assertEquals(null, keyStringUtil.getNamespace());
        assertEquals(KeyType.SHARED_KEY, keyStringUtil.getKeyType());
        assertEquals("@bob", keyStringUtil.getSharedBy());
        assertEquals("@alice", keyStringUtil.getSharedWith());
        assertEquals(false, keyStringUtil.isCached());
        assertEquals(true, keyStringUtil.isHidden());
    }

    // Row 14A Private keys
    @Test
    public void privateKey1() {
        String KEY_NAME = "private:phone@bob";
        KeyStringUtil keyStringUtil = new KeyStringUtil(KEY_NAME);
        assertEquals("private:phone@bob", keyStringUtil.getFullKeyName());
        assertEquals("phone", keyStringUtil.getKeyName());
        assertEquals(null, keyStringUtil.getNamespace());
        assertEquals(KeyType.PRIVATE_HIDDEN_KEY, keyStringUtil.getKeyType());
        assertEquals("@bob", keyStringUtil.getSharedBy());
        assertEquals(null, keyStringUtil.getSharedWith());
        assertEquals(false, keyStringUtil.isCached());
        assertEquals(true, keyStringUtil.isHidden());
    }

    // Row 14B Private keys
    @Test
    public void privateKey2() {
        String KEY_NAME = "privatekey:phone@bob";
        KeyStringUtil keyStringUtil = new KeyStringUtil(KEY_NAME);
        assertEquals("privatekey:phone@bob", keyStringUtil.getFullKeyName());
        assertEquals("phone", keyStringUtil.getKeyName());
        assertEquals(null, keyStringUtil.getNamespace());
        assertEquals(KeyType.PRIVATE_HIDDEN_KEY, keyStringUtil.getKeyType());
        assertEquals("@bob", keyStringUtil.getSharedBy());
        assertEquals(null, keyStringUtil.getSharedWith());
        assertEquals(false, keyStringUtil.isCached());
        assertEquals(true, keyStringUtil.isHidden());
    }

    /**
     *  0:  @farinataanxious:lemon@sportsunconscious
     *  1:  @farinataanxious:shared_key@sportsunconscious
     *  2:  @farinataanxious:test@sportsunconscious
     *  3:  @sportsunconscious:shared_key@sportsunconscious        
     *  4:  @sportsunconscious:signing_privatekey@sportsunconscious
     *  5:  public:publickey@farinataanxious
     *  6:  public:publickey@sportsunconscious
     *  7:  public:signing_publickey@sportsunconscious
     *  8:  shared_key.farinataanxious@sportsunconscious
     *  9:  shared_key.sportsunconscious@sportsunconscious
     */
    @Test
    public void suKey1() {
        String KEY_NAME = "@farinataanxious:lemon@sportsunconscious";
        KeyStringUtil keyStringUtil = new KeyStringUtil(KEY_NAME);
        assertEquals("@farinataanxious:lemon@sportsunconscious", keyStringUtil.getFullKeyName());
        assertEquals("lemon", keyStringUtil.getKeyName());
        assertEquals(null, keyStringUtil.getNamespace());
        assertEquals(KeyType.SHARED_KEY, keyStringUtil.getKeyType());
        assertEquals("@sportsunconscious", keyStringUtil.getSharedBy());
        assertEquals("@farinataanxious", keyStringUtil.getSharedWith());
        assertEquals(false, keyStringUtil.isCached());
        assertEquals(false, keyStringUtil.isHidden());
    }

    @Test
    public void suKey2() {
        String KEY_NAME = "@farinataanxious:shared_key@sportsunconscious";
        KeyStringUtil keyStringUtil = new KeyStringUtil(KEY_NAME);
        assertEquals("@farinataanxious:shared_key@sportsunconscious", keyStringUtil.getFullKeyName());
        assertEquals(null, keyStringUtil.getNamespace());
        assertEquals("shared_key", keyStringUtil.getKeyName());
        assertEquals(KeyType.SHARED_KEY, keyStringUtil.getKeyType());
        assertEquals("@sportsunconscious", keyStringUtil.getSharedBy());
        assertEquals("@farinataanxious", keyStringUtil.getSharedWith());
        assertEquals(false, keyStringUtil.isCached());
        assertEquals(false, keyStringUtil.isHidden());
    }

    @Test
    public void suKey3() {
        String KEY_NAME = "@farinataanxious:test@sportsunconscious";
        KeyStringUtil keyStringUtil = new KeyStringUtil(KEY_NAME);
        assertEquals("@farinataanxious:test@sportsunconscious", keyStringUtil.getFullKeyName());
        assertEquals("test", keyStringUtil.getKeyName());
        assertEquals(null, keyStringUtil.getNamespace());
        assertEquals(KeyType.SHARED_KEY, keyStringUtil.getKeyType());
        assertEquals("@sportsunconscious", keyStringUtil.getSharedBy());
        assertEquals("@farinataanxious", keyStringUtil.getSharedWith());
        assertEquals(false, keyStringUtil.isCached());
        assertEquals(false, keyStringUtil.isHidden());
    }

    @Test
    public void suKey4() {
        String KEY_NAME = "@sportsunconscious:shared_key@sportsunconscious";
        KeyStringUtil keyStringUtil = new KeyStringUtil(KEY_NAME);
        assertEquals("@sportsunconscious:shared_key@sportsunconscious", keyStringUtil.getFullKeyName());
        assertEquals("shared_key", keyStringUtil.getKeyName());
        assertEquals(null, keyStringUtil.getNamespace());
        assertEquals(KeyType.SELF_KEY, keyStringUtil.getKeyType());
        assertEquals("@sportsunconscious", keyStringUtil.getSharedBy());
        assertEquals("@sportsunconscious", keyStringUtil.getSharedWith());
        assertEquals(false, keyStringUtil.isCached());
        assertEquals(false, keyStringUtil.isHidden());
    }

    @Test
    public void suKey5() {
        String KEY_NAME = "@sportsunconscious:signing_privatekey@sportsunconscious";
        KeyStringUtil keyStringUtil = new KeyStringUtil(KEY_NAME);
        assertEquals("@sportsunconscious:signing_privatekey@sportsunconscious", keyStringUtil.getFullKeyName());
        assertEquals("signing_privatekey", keyStringUtil.getKeyName());
        assertEquals(null, keyStringUtil.getNamespace());
        assertEquals(KeyType.SELF_KEY, keyStringUtil.getKeyType());
        assertEquals("@sportsunconscious", keyStringUtil.getSharedBy());
        assertEquals("@sportsunconscious", keyStringUtil.getSharedWith());
        assertEquals(false, keyStringUtil.isCached());
        assertEquals(false, keyStringUtil.isHidden());
    }

    @Test
    public void suKey6() {
        String KEY_NAME = "public:publickey@farinataanxious";
        KeyStringUtil keyStringUtil = new KeyStringUtil(KEY_NAME);
        assertEquals("public:publickey@farinataanxious", keyStringUtil.getFullKeyName());
        assertEquals("publickey", keyStringUtil.getKeyName());
        assertEquals(null, keyStringUtil.getNamespace());
        assertEquals(KeyType.PUBLIC_KEY, keyStringUtil.getKeyType());
        assertEquals("@farinataanxious", keyStringUtil.getSharedBy());
        assertEquals(null, keyStringUtil.getSharedWith());
        assertEquals(false, keyStringUtil.isCached());
        assertEquals(false, keyStringUtil.isHidden());
    }

    @Test
    public void suKey7() {
        String KEY_NAME = "public:publickey@sportsunconscious";
        KeyStringUtil keyStringUtil = new KeyStringUtil(KEY_NAME);
        assertEquals("public:publickey@sportsunconscious", keyStringUtil.getFullKeyName());
        assertEquals("publickey", keyStringUtil.getKeyName());
        assertEquals(null, keyStringUtil.getNamespace());
        assertEquals(KeyType.PUBLIC_KEY, keyStringUtil.getKeyType());
        assertEquals("@sportsunconscious", keyStringUtil.getSharedBy());
        assertEquals(null, keyStringUtil.getSharedWith());
        assertEquals(false, keyStringUtil.isCached());
        assertEquals(false, keyStringUtil.isHidden());
    }

    @Test
    public void suKey8() {
        String KEY_NAME = "shared_key.farinataanxious@sportsunconscious";
        KeyStringUtil keyStringUtil = new KeyStringUtil(KEY_NAME);
        assertEquals("shared_key.farinataanxious@sportsunconscious", keyStringUtil.getFullKeyName());
        assertEquals("shared_key.farinataanxious", keyStringUtil.getKeyName());
        assertEquals(null, keyStringUtil.getNamespace());
        assertEquals(KeyType.SELF_KEY, keyStringUtil.getKeyType());
        assertEquals("@sportsunconscious", keyStringUtil.getSharedBy());
        assertEquals(null, keyStringUtil.getSharedWith());
        assertEquals(false, keyStringUtil.isCached());
        assertEquals(false, keyStringUtil.isHidden());
    }

    @Test
    public void suKey9() {
        String KEY_NAME = "atconnections.hacktheleague.smoothalligator.at_contact.mospherepro.hacktheleague@smoothalligator";
        KeyStringUtil keyStringUtil = new KeyStringUtil(KEY_NAME);
        assertEquals("atconnections.hacktheleague.smoothalligator.at_contact.mospherepro.hacktheleague@smoothalligator", keyStringUtil.getFullKeyName());
        assertEquals("atconnections.hacktheleague.smoothalligator.at_contact.mospherepro.hacktheleague", keyStringUtil.getKeyName());
        assertEquals(null, keyStringUtil.getNamespace());

    }

    /**
     * smoothalligator
     * 0:  @abbcservicesinc:shared_key@smoothalligator
     * 1:  @denise:shared_key@smoothalligator
     * 2:  @er_nobile_14:shared_key@smoothalligator
     * 3:  @fascinatingsnow:shared_key@smoothalligator
     * 4:  @hacktheleague:shared_key@smoothalligator
     * 5:  @smoothalligator:signing_privatekey@smoothalligator
     * 6:  @wildgreen:shared_key@smoothalligator
     * 7:  atconnections.abbcservicesinc.smoothalligator.at_contact.mospherepro.abbcservicesinc@smoothalligator
     * 8:  atconnections.denise.smoothalligator.at_contact.mospherepro.denise@smoothalligator
     * 9:  atconnections.hacktheleague.smoothalligator.at_contact.mospherepro.hacktheleague@smoothalligator
     * 10:  atconnections.wildgreen.smoothalligator.at_contact.mospherepro.wildgreen@smoothalligator
     * 11:  @smoothalligator:shared_key@abbcservicesinc
     * 12:  @smoothalligator:shared_key@denise
     * 13:  @smoothalligator:shared_key@fascinatingsnow
     * 14:  @smoothalligator:shared_key@wildgreen
     * 15:  public:firstname.wavi.wavi@abbcservicesinc
     * 16:  public:firstname.wavi.wavi@wildgreen
     * 17:  public:image.wavi.wavi@abbcservicesinc
     * 18:  public:image.wavi.wavi@denise
     * 19:  public:image.wavi.wavi@wildgreen
     * 20:  public:lastname.wavi.wavi@abbcservicesinc
     * 21:  public:lastname.wavi.wavi@wildgreen
     * 22:  public:publickey@abbcservicesinc
     * 23:  public:publickey@denise
     * 24:  public:publickey@er_nobile_14
     * 25:  public:publickey@fascinatingsnow
     * 26:  public:publickey@hacktheleague
     * 27:  public:publickey@wildgreen
     * 28:  public:email.wavi.wavi@smoothalligator
     * 29:  public:field_order_of_self.wavi.wavi@smoothalligator
     * 30:  public:firstname.wavi.wavi@smoothalligator
     * 31:  public:following_by_self.at_follows.wavi.at_follows@smoothalligator
     * 32:  public:lastname.wavi.wavi@smoothalligator
     * 33:  public:privateaccount.wavi.wavi@smoothalligator
     * 34:  public:publickey@smoothalligator
     * 35:  public:signing_publickey@smoothalligator
     * 36:  public:theme_color.wavi.wavi@smoothalligator
     * 37:  publickey.fascinatingsnow.fascinatingsnow@smoothalligator
     * 38:  senthistory_v2.mospherepro.mospherepro@smoothalligator
     * 39:  shared_key.abbcservicesinc@smoothalligator
     * 40:  shared_key.denise@smoothalligator
     * 41:  shared_key.er_nobile_14@smoothalligator
     * 42:  shared_key.fascinatingsnow@smoothalligator
     * 43:  shared_key.hacktheleague@smoothalligator
     * 44:  shared_key.wildgreen@smoothalligator
     */

    @Test
    public void saTest1() {
        String KEY_NAME = "@abbcservicesinc:shared_key@smoothalligator";
        KeyStringUtil keyStringUtil = new KeyStringUtil(KEY_NAME);
        assertEquals("@abbcservicesinc:shared_key@smoothalligator", keyStringUtil.getFullKeyName());
        assertEquals("shared_key", keyStringUtil.getKeyName());
        assertEquals(KeyType.SHARED_KEY, keyStringUtil.getKeyType());
        assertEquals("@smoothalligator", keyStringUtil.getSharedBy());
        assertEquals("@abbcservicesinc", keyStringUtil.getSharedWith());
        assertEquals(false, keyStringUtil.isCached());
        assertEquals(false, keyStringUtil.isHidden());
    }
    
    @Test
    public void saTest2() {
        String KEY_NAME = "@denise:shared_key@smoothalligator";
        KeyStringUtil keyStringUtil = new KeyStringUtil(KEY_NAME);
        assertEquals("@denise:shared_key@smoothalligator", keyStringUtil.getFullKeyName());
        assertEquals("shared_key", keyStringUtil.getKeyName());
        assertEquals(KeyType.SHARED_KEY, keyStringUtil.getKeyType());
        assertEquals("@smoothalligator", keyStringUtil.getSharedBy());
        assertEquals("@denise", keyStringUtil.getSharedWith());
        assertEquals(false, keyStringUtil.isCached());
        assertEquals(false, keyStringUtil.isHidden());
    }

    @Test
    public void saTest3() {
        String KEY_NAME = "@er_nobile_14:shared_key@smoothalligator";
        KeyStringUtil keyStringUtil = new KeyStringUtil(KEY_NAME);
        assertEquals("@er_nobile_14:shared_key@smoothalligator", keyStringUtil.getFullKeyName());
        assertEquals("shared_key", keyStringUtil.getKeyName());
        assertEquals(null, keyStringUtil.getNamespace());
        assertEquals(KeyType.SHARED_KEY, keyStringUtil.getKeyType());
        assertEquals("@smoothalligator", keyStringUtil.getSharedBy());
        assertEquals("@er_nobile_14", keyStringUtil.getSharedWith());
        assertEquals(false, keyStringUtil.isCached());
        assertEquals(false, keyStringUtil.isHidden());
        assertEquals(null, keyStringUtil.getNamespace());
    }

    @Test
    public void saTest4() {
        String KEY_NAME = "@fascinatingsnow:shared_key@smoothalligator";
        KeyStringUtil keyStringUtil = new KeyStringUtil(KEY_NAME);
        assertEquals("@fascinatingsnow:shared_key@smoothalligator", keyStringUtil.getFullKeyName());
        assertEquals("shared_key", keyStringUtil.getKeyName());
        assertEquals(null, keyStringUtil.getNamespace());
        assertEquals(KeyType.SHARED_KEY, keyStringUtil.getKeyType());
        assertEquals("@smoothalligator", keyStringUtil.getSharedBy());
        assertEquals("@fascinatingsnow", keyStringUtil.getSharedWith());
        assertEquals(false, keyStringUtil.isCached());
        assertEquals(false, keyStringUtil.isHidden());
    }

    @Test
    public void saTest7() {
        String KEY_NAME = "atconnections.abbcservicesinc.smoothalligator.at_contact.mospherepro.abbcservicesinc@smoothalligator";
        KeyStringUtil keyStringUtil = new KeyStringUtil(KEY_NAME);
        assertEquals("atconnections.abbcservicesinc.smoothalligator.at_contact.mospherepro.abbcservicesinc@smoothalligator", keyStringUtil.getFullKeyName());
        assertEquals("atconnections.abbcservicesinc.smoothalligator.at_contact.mospherepro.abbcservicesinc", keyStringUtil.getKeyName());
        assertEquals(null, keyStringUtil.getNamespace());
        assertEquals(KeyType.SELF_KEY, keyStringUtil.getKeyType());
        assertEquals("@smoothalligator", keyStringUtil.getSharedBy());
        assertEquals(null, keyStringUtil.getSharedWith());
        assertEquals(false, keyStringUtil.isCached());
        assertEquals(false, keyStringUtil.isHidden());
        assertEquals(null, keyStringUtil.getNamespace());
    }
}