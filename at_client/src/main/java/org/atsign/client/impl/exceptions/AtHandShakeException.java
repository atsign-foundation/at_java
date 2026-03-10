package org.atsign.client.impl.exceptions;

/**
 * This exception is for any exception during the handshake process of two atServers.
 */
public class AtHandShakeException extends AtException {

  public static final String CODE = "AT0008";

  public AtHandShakeException(String message) {
    super(message);
  }
}
