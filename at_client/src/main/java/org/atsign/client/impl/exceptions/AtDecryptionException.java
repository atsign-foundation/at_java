package org.atsign.client.impl.exceptions;

/**
 * Occurs when attempt to decrypt fails
 */
public class AtDecryptionException extends AtException {
  public AtDecryptionException(String message, Throwable cause) {
    super(message, cause);
  }
}
