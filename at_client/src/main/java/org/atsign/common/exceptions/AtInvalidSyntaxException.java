package org.atsign.common.exceptions;

import org.atsign.common.AtException;

/**
 * Exception occurs if we give any invalid command to the server.
 */
public class AtInvalidSyntaxException extends AtException {

  public static final String CODE = "AT0003";

  public AtInvalidSyntaxException(String message) {
    super(message);
  }
}
