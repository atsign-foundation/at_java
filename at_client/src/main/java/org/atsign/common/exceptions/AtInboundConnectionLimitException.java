package org.atsign.common.exceptions;

import org.atsign.common.AtException;

public class AtInboundConnectionLimitException extends AtException {
    public AtInboundConnectionLimitException(String message) {
        super(message);
    }
}
