package org.atsign.common;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.atsign.client.util.AtClientValidation;
import org.atsign.common.Keys.PublicKey;
import org.atsign.common.Keys.SelfKey;
import org.atsign.common.Keys.SharedKey;
import org.atsign.cucumber.helpers.Helpers;
import org.atsign.virtualenv.VirtualEnv;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class AtClientValidationIT {

  private static final String VE_ROOT_URL = "vip.ve.atsign.zone:64"; // prod rootUrl

  @BeforeAll
  public static void classSetup() {
    if (!Helpers.isHostPortReachable(VE_ROOT_URL, SECONDS.toMillis(2))) {
      VirtualEnv.setUp();
    }
  }

  // atSign exists (uses secondaryaddress.finder)
  @Test
  public void atSignExistsTest() {

    // null atSign
    assertThrows(AtException.class, () -> {
      AtSign atSign = null;
      AtClientValidation.atSignExists(atSign, VE_ROOT_URL);
    });

    // empty atSign
    assertThrows(IllegalArgumentException.class, () -> {
      AtSign atSign = new AtSign("");
      AtClientValidation.atSignExists(atSign, VE_ROOT_URL);
    });

    // root does not contain atSign
    assertThrows(AtException.class, () -> {
      AtSign atSign = new AtSign("someAtSignThatDNE456");
      AtClientValidation.atSignExists(atSign, VE_ROOT_URL);
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

    // ====================================
    // PublicKey tests
    // ====================================

    // null publicKey
    assertThrows(AtException.class, () -> {
      PublicKey publicKey = null;
      AtClientValidation.validateAtKey(publicKey, VE_ROOT_URL);
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
      AtClientValidation.validateAtKey(selfKey, VE_ROOT_URL);
    });

    // self key with invalid keyName
    assertThrows(AtException.class, () -> {
      AtSign sharedBy = new AtSign("@bob");
      SelfKey selfKey = new KeyBuilders.SelfKeyBuilder(sharedBy).key("t est").build();
      AtClientValidation.validateAtKey(selfKey, VE_ROOT_URL);
    });

    // self key with non-existent sharedWith atSign in root
    assertThrows(AtException.class, () -> {
      AtSign sharedBy = new AtSign("@nonexistentatsign");
      AtSign sharedWith = new AtSign("@nonexistentatsign"); // atSign does not exist in root
      SelfKey selfKey = new KeyBuilders.SelfKeyBuilder(sharedBy, sharedWith).key("test").build();
      AtClientValidation.validateAtKey(selfKey, VE_ROOT_URL);
    });

    // ====================================
    // SharedKey tests
    // ====================================

    // null shared key test
    assertThrows(AtException.class, () -> {
      SharedKey sharedKey = null;
      AtClientValidation.validateAtKey(sharedKey, VE_ROOT_URL);
    });

    // shared key with ttr < -1
    assertThrows(AtException.class, () -> {
      AtSign sharedBy = new AtSign("@bob");
      AtSign sharedWith = new AtSign("@alice");
      SharedKey sharedKey = new KeyBuilders.SharedKeyBuilder(sharedBy, sharedWith).key("test").build();
      sharedKey.metadata.ttr = -22323;
      AtClientValidation.validateAtKey(sharedKey, VE_ROOT_URL);
    });

    // shared key with invalid keyName
    assertThrows(AtException.class, () -> {
      AtSign sharedBy = new AtSign("@bob");
      AtSign sharedWith = new AtSign("@alice");
      SharedKey sharedKey = new KeyBuilders.SharedKeyBuilder(sharedBy, sharedWith).key("t est").build();
      AtClientValidation.validateAtKey(sharedKey, VE_ROOT_URL);
    });

    // shared key with invalid sharedWith atSign (atSign dne in root)
    assertThrows(AtException.class, () -> {
      AtSign sharedBy = new AtSign("@wildgreen");
      AtSign sharedWith = new AtSign("@nonexistentatsign"); // atSign does not exist in root
      SharedKey sharedKey = new KeyBuilders.SharedKeyBuilder(sharedBy, sharedWith).key("test").build();
      AtClientValidation.validateAtKey(sharedKey, VE_ROOT_URL);
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
}
