package org.atsign.client.impl.exceptions;

/**
 * Occurs if response cannot be decoded
 */
public class AtResponseHandlingException extends AtException {

  public AtResponseHandlingException(String message) {
    super(message);
  }

  public AtResponseHandlingException(String message, Throwable cause) {
    super(message, cause);
  }
}
