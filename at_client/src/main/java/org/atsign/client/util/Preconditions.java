package org.atsign.client.util;

import java.io.File;
import java.util.function.Predicate;

/**
 * Basic precondition helpers (we could use something like guava but we want to limit dependencies)
 */
public class Preconditions {

    public static <T> T checkNotNull(T instance, String message) {
        if (instance == null) {
            throw new RuntimeException(message);
        }
        return instance;
    }

    public static <T> T checkNotNull(T instance) {
        return checkNotNull(instance, "null");
    }

    public static File checkFile(File f, Predicate<File> predicate, String message) {
      if (!predicate.test(f)) {
          throw new IllegalArgumentException(message);
      }
      return f;
  }

}
