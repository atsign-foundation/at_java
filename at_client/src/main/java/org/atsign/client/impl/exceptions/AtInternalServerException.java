package org.atsign.client.impl.exceptions;

/**
 * This exception is used for any server related exceptions.
 */
public class AtInternalServerException extends AtException {

  public static final String CODE = "AT0011";

  public AtInternalServerException(String message) {
    super(message);
  }
}
