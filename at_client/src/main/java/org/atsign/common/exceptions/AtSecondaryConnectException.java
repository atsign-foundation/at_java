package org.atsign.common.exceptions;

import org.atsign.common.AtException;

/**
 *
 */
public class AtSecondaryConnectException extends AtException {
    public AtSecondaryConnectException(String message) {
        super(message);
    }

    public AtSecondaryConnectException(String message, Throwable cause) {
        super(message, cause);
    }
}
