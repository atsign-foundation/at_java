package org.atsign.client.impl.exceptions;

/**
 * Occurs when AtKey string is null, the wrong format or contains unsupported characters
 */
public class AtInvalidAtKeyException extends AtException {

  public static final String CODE = "AT0016";

  public AtInvalidAtKeyException(String message) {
    super(message);
  }
}
