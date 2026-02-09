package org.atsign.common;

/**
 * Base class for all Atsign Platform {@link Exception}s
 */
public abstract class AtException extends Exception {
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
