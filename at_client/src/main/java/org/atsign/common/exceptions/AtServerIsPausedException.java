package org.atsign.common.exceptions;

import org.atsign.common.AtException;

/**
 * Occurs when the AtServer has been paused
 */
public class AtServerIsPausedException extends AtException {

  public static final String CODE = "AT0024";

  public AtServerIsPausedException(String message) {
    super(message);
  }
}
