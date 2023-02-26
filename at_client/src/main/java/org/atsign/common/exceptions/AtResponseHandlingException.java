package org.atsign.common.exceptions;

import org.atsign.common.AtException;

public class AtResponseHandlingException extends AtException {
    public AtResponseHandlingException(String message) {
        super(message);
    }

    public AtResponseHandlingException(String message, Throwable cause) {
        super(message, cause);
    }
}
