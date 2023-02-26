package org.atsign.common;

public abstract class AtException extends Exception {
    public AtException(String message) {super(message);}

    public AtException(String message, Throwable cause) {
        super(message, cause);
    }

    @Override
    public String toString() {
        return super.toString() + (getCause() != null ? getCause().toString() : "");
    }
}
