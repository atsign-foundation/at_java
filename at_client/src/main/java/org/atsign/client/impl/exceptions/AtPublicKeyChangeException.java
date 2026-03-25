package org.atsign.client.impl.exceptions;

/**
 * Occurs when sharedKeyEnc was encrypted with a public key that has now changed
 */
public class AtPublicKeyChangeException extends AtException {
  public AtPublicKeyChangeException(String message) {
    super(message);
  }

  public AtPublicKeyChangeException(String message, Throwable cause) {
    super(message, cause);
  }
}
