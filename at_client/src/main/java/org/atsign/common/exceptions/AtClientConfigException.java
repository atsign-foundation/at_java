package org.atsign.common.exceptions;

import org.atsign.common.AtException;

/**
 *
 */
public class AtClientConfigException extends AtException {
    public AtClientConfigException(String message) {
        super(message);
    }

    public AtClientConfigException(String message, Throwable cause) {
        super(message, cause);
    }
}
