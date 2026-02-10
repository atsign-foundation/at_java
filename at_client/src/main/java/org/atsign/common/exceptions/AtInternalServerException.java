package org.atsign.common.exceptions;

import org.atsign.common.AtException;

/**
 * This exception is used for any server related exceptions.
 */
public class AtInternalServerException extends AtException {

  public static final String CODE = "AT0011";

  public AtInternalServerException(String message) {
    super(message);
  }
}
