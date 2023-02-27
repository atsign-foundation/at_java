package org.atsign.common.exceptions;

import org.atsign.common.AtException;

/**
 *
 */
public class AtSecondaryNotFoundException extends AtException {
    public AtSecondaryNotFoundException(String message) {
        super(message);
    }

    public AtSecondaryNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}
