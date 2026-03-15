package org.atsign.client.impl.exceptions;

/**
 * This is for any server related errors.
 */
public class AtInternalServerError extends AtException {

  public static final String CODE = "AT0010";

  public AtInternalServerError(String message) {
    super(message);
  }
}
