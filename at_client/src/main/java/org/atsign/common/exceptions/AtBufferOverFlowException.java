package org.atsign.common.exceptions;

import org.atsign.common.AtException;

/**
 * This exception occurs when input/output message size reaches the maximum limit configured in the
 * server.
 */
public class AtBufferOverFlowException extends AtException {

  public static final String CODE = "AT0005";

  public AtBufferOverFlowException(String message) {
    super(message);
  }
}
