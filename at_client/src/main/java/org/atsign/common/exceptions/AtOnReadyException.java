package org.atsign.common.exceptions;

import java.util.function.Consumer;

/**
 * A {@link RuntimeException} that is used to communicate fatal exception
 * in the execution of an OnReady consumer.
 * See {@link org.atsign.client.connection.api.AtClientConnection#onReady(Consumer)}.
 */
public class AtOnReadyException extends RuntimeException {

  public AtOnReadyException(String message) {
    super(message);
  }

  public AtOnReadyException(String message, Throwable cause) {
    super(message, cause);
  }
}
