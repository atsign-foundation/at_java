package org.atsign.client.util;

import java.io.IOException;

import org.atsign.client.api.Secondary;
import org.atsign.common.AtException;
import org.atsign.common.AtSign;
import org.atsign.common.exceptions.AtIllegalArgumentException;
import org.atsign.common.exceptions.AtSecondaryConnectException;

/**
 * Utility class with key string validation methods
 */
public class AtClientValidation {

  /**
   * Checks if an atSign exists on a root server
   *
   * @param atSign atSign object to check for existence
   * @param rootUrl rootUrl of the root server for atSign existence
   * @throws AtException if atSign does not exist/address could not be found, if atSign object is
   *         empty,
   */
  public static void atSignExists(AtSign atSign, String rootUrl) throws AtException {
    if (atSign == null || atSign.toString().isEmpty()) {
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

}
