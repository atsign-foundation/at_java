package org.atsign.client.impl.exceptions;

/**
 * Occurs if response does not have a recognized structure
 */
public class AtUnknownResponseException extends AtException {
  public AtUnknownResponseException(String message) {
    super(message);
  }
}
