package org.atsign.common.exceptions;

import org.atsign.common.AtException;

/**
 * Will occur when an unsuccessful handshake happens between two atServers.
 */
public class AtUnauthorizedException extends AtException {

  public static final String CODE = "AT0009";

  public AtUnauthorizedException(String message) {
    super(message);
  }
}
