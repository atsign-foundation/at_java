package org.atsign.common.exceptions;

import org.atsign.common.AtException;

/**
 * Occurs if response does not have a recognized structure
 */
public class AtUnknownResponseException extends AtException {
  public AtUnknownResponseException(String message) {
    super(message);
  }
}
