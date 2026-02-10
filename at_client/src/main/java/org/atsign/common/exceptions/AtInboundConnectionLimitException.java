package org.atsign.common.exceptions;

import org.atsign.common.AtException;

/**
 * This exception will occur when the number of active clients reaches the maximum limit configured.
 */
public class AtInboundConnectionLimitException extends AtException {

  public static final String CODE = "AT0012";

  public AtInboundConnectionLimitException(String message) {
    super(message);
  }
}
