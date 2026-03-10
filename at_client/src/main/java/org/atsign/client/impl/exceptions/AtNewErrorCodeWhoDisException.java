package org.atsign.client.impl.exceptions;

/**
 * Server exception with unrecognized error code
 */
public class AtNewErrorCodeWhoDisException extends AtException {
  public AtNewErrorCodeWhoDisException(String errorCode, String message) {
    super(errorCode + ":" + message);
  }
}
