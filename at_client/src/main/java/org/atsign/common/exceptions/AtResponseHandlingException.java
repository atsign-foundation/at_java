package org.atsign.common.exceptions;

import org.atsign.common.AtException;

/**
 * Occurs if response from {@link org.atsign.client.api.Secondary} cannot be decoded
 */
public class AtResponseHandlingException extends AtException {

  public AtResponseHandlingException(String message) {
    super(message);
  }

  public AtResponseHandlingException(String message, Throwable cause) {
    super(message, cause);
  }
}
