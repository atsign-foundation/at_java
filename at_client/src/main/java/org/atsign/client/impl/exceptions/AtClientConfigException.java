package org.atsign.client.impl.exceptions;

/**
 * Occurs when configuration required to create an {@link org.atsign.client.api.AtClient} is
 * incorrect or missing
 */
public class AtClientConfigException extends AtException {
  public AtClientConfigException(String message) {
    super(message);
  }

  public AtClientConfigException(String message, Throwable cause) {
    super(message, cause);
  }
}
