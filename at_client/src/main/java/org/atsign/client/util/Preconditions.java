package org.atsign.client.util;

public class Preconditions {

    public static <T> T checkNotNull(T instance, String message) {
        if (instance == null) {
            throw new RuntimeException(message);
        }
        return instance;
    }
}
