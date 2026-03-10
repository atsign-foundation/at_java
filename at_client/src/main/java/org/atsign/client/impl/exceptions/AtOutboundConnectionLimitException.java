package org.atsign.client.impl.exceptions;

/**
 * Exception occurs when the number of open connections to other atServers reaches the maximum limit
 * configured.
 */
public class AtOutboundConnectionLimitException extends AtException {

  public static final String CODE = "AT0006";

  public AtOutboundConnectionLimitException(String message) {
    super(message);
  }
}
