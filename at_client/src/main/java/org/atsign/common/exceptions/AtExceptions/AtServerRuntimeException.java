package org.atsign.common.exceptions.AtExceptions;

import org.atsign.common.AtException;

/**
 * Exception occurs when there is an issue while starting the server.
 */
public class AtServerRuntimeException extends AtException {

  public static final String CODE = "AT0001";

  public AtServerRuntimeException(String message) {
    super(message);
  }
}
