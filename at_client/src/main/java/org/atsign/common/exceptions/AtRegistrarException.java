package org.atsign.common.exceptions;

import org.atsign.common.AtException;

public class AtRegistrarException extends AtException {
    public AtRegistrarException(String message) {
        super(message);
    }

    public AtRegistrarException(String message, Throwable cause) {
        super(message, cause);
    }
}
