package org.atsign.client.impl.exceptions;

/**
 * Occurs when command argument provided is invalid
 */
public class AtIllegalArgumentException extends AtException {

  public static final String CODE = "AT0022";

  public AtIllegalArgumentException(String message) {
    super(message);
  }
}
