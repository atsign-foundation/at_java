package org.atsign.common.exceptions;

import org.atsign.common.AtException;

/**
 * Occurs if AtKey string cannot be decoded
 */
public class MalformedKeyException extends AtException {
  public MalformedKeyException(String message) {
    super(message);
  }
}
