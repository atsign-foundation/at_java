package org.atsign.client.impl.exceptions;

/**
 * Occurs if AtKey string cannot be decoded
 */
public class MalformedKeyException extends AtException {
  public MalformedKeyException(String message) {
    super(message);
  }
}
