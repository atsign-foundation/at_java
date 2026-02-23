package org.atsign.common;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.atsign.client.util.AtClientValidation;
import org.atsign.common.exceptions.AtSecondaryConnectException;
import org.atsign.common.exceptions.AtSecondaryNotFoundException;
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
    assertThrows(AtSecondaryNotFoundException.class, () -> {
      AtSign atSign = new AtSign("");
      AtClientValidation.atSignExists(atSign, VE_ROOT_URL);
    });

    // root does not contain atSign
    assertThrows(AtSecondaryNotFoundException.class, () -> {
      AtSign atSign = new AtSign("someAtSignThatDNE456");
      AtClientValidation.atSignExists(atSign, VE_ROOT_URL);
    });

    // invalid root
    assertThrows(AtSecondaryConnectException.class, () -> {
      AtSign atSign = new AtSign("smoothalligator");
      AtClientValidation.atSignExists(atSign, "invalidroot:32123");
    });

  }
}
