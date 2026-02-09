package org.atsign.common.exceptions;

import org.atsign.common.AtException;

/**
 * Server exception with unrecognized error code
 */
public class AtNewErrorCodeWhoDisException extends AtException {
  public AtNewErrorCodeWhoDisException(String errorCode, String message) {
    super(errorCode + ":" + message);
  }
}
