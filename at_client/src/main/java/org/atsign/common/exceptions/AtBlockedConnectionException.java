package org.atsign.common.exceptions;

import org.atsign.common.AtException;

/**
 * This will occur when a blocked user tries to connect to the atServer.
 */
public class AtBlockedConnectionException extends AtException {

  public static final String CODE = "AT0013";

  public AtBlockedConnectionException(String message) {
    super(message);
  }
}
