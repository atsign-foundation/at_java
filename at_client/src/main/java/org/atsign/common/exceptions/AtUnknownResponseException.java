package org.atsign.common.exceptions;

import org.atsign.common.AtException;

/**
 * Occurs if response from {@link org.atsign.client.api.Secondary} does not have a recognized
 * structure
 */
public class AtUnknownResponseException extends AtException {
  public AtUnknownResponseException(String message) {
    super(message);
  }
}
