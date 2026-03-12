package org.atsign.client.impl.exceptions;

/**
 * Base class for all Atsign Platform {@link Exception}s
 */
public class AtException extends Exception {

  public AtException(String message) {
    super(message);
  }

  public AtException(String message, Throwable cause) {
    super(message, cause);
  }

  @Override
  public String toString() {
    return super.toString() + (getCause() != null ? getCause().toString() : "");
  }
}
