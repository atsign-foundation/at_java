package org.atsign.common;

public class NoSuchSecondaryException extends AtException {
    public NoSuchSecondaryException(String message) {
        super(message);
    }

    public NoSuchSecondaryException(String message, Throwable cause) {
        super(message, cause);
    }
}
