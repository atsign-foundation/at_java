package org.atsign.common.exceptions;

import org.atsign.common.AtException;

/**
 * Occurs when attempt to decrypt fails
 */
public class AtDecryptionException extends AtException {
  public AtDecryptionException(String message, Throwable cause) {
    super(message, cause);
  }
}
