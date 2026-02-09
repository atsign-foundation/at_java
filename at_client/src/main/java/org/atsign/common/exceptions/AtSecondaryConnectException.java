package org.atsign.common.exceptions;

import org.atsign.common.AtException;

/**
 * This exception will occur when we are unable to connect to an atServer.
 */
public class AtSecondaryConnectException extends AtException {

  public static final String CODE = "AT0021";

  public AtSecondaryConnectException(String message) {
    super(message);
  }

  public AtSecondaryConnectException(String message, Throwable cause) {
    super(message, cause);
  }
}
