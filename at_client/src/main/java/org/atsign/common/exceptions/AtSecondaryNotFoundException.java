package org.atsign.common.exceptions;

import org.atsign.common.AtException;

/**
 * Exception occurs when an atServer tries to connect to another atServer which is not available in
 * the
 * atDirectory or not yet instantiated.
 */
public class AtSecondaryNotFoundException extends AtException {

  public static final String CODE = "AT0007";

  public AtSecondaryNotFoundException(String message) {
    super(message);
  }

  public AtSecondaryNotFoundException(String message, Throwable cause) {
    super(message, cause);
  }
}
