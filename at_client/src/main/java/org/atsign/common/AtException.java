package org.atsign.common;

public class AtException extends Exception {
    public AtException(String message) {
        super(message);
    }

    public AtException(String message, Throwable cause) {
        super(message, cause);
    }
}
