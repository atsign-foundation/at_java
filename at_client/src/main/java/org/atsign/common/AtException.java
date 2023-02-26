package org.atsign.common;

public abstract class AtException extends Exception {
    public AtException(String message) {super(message);}

    public AtException(String message, Throwable cause) {
        super(message, cause);
    }
}
