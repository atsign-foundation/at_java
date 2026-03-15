package org.atsign.client.impl.exceptions;

/**
 * Occurs if there is an exception during registration
 */
public class AtRegistrarException extends AtException {

  public AtRegistrarException(String message) {
    super(message);
  }

  public AtRegistrarException(String message, Throwable cause) {
    super(message, cause);
  }
}
