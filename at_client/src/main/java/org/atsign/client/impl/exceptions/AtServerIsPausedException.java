package org.atsign.client.impl.exceptions;

/**
 * Occurs when the AtServer has been paused
 */
public class AtServerIsPausedException extends AtException {

  public static final String CODE = "AT0024";

  public AtServerIsPausedException(String message) {
    super(message);
  }
}
