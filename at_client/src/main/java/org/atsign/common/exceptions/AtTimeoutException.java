package org.atsign.common.exceptions;

import org.atsign.common.AtException;

/**
 * Exception occurs when there is a timeout in the server.
 */
public class AtTimeoutException extends AtException {

  public static final String CODE = "AT0023";

  public AtTimeoutException(String message) {
    super(message);
  }
}
