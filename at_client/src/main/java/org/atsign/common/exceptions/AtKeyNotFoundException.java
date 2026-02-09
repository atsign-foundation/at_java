package org.atsign.common.exceptions;

import org.atsign.common.AtException;

/**
 * This exception will be thrown when the key is not available for encryption/decryption.
 */
public class AtKeyNotFoundException extends AtException {

  public static final String CODE = "AT0015";

  public AtKeyNotFoundException(String message) {
    super(message);
  }
}
