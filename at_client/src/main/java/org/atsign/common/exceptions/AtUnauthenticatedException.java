package org.atsign.common.exceptions;

import org.atsign.common.AtException;

/**
 * This exception occurs when client authentication fails or client tries to execute any verb which
 * needs
 * authentication.
 */
public class AtUnauthenticatedException extends AtException {

  public static final String CODE = "AT0401";

  public AtUnauthenticatedException(String message) {
    super(message);
  }
}
