package org.atsign.common.exceptions;

import org.atsign.common.AtException;

/**
 * Occurs when {@link org.atsign.client.api.Secondary} has been paused
 */
public class AtServerIsPausedException extends AtException {

  public static final String CODE = "AT0024";

  public AtServerIsPausedException(String message) {
    super(message);
  }
}
