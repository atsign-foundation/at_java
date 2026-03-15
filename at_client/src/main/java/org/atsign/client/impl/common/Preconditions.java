package org.atsign.client.impl.common;

import java.io.File;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Basic precondition helpers.
 */
public class Preconditions {

  public static <T> T checkNotNull(T instance, String message) {
    if (instance == null) {
      throw new IllegalArgumentException(message);
    }
    return instance;
  }

  public static <T> T checkNotNull(T instance) {
    return checkNotNull(instance, "null");
  }

  public static void checkNull(Object instance, String message) {
    if (instance != null) {
      throw new IllegalArgumentException(message);
    }
  }

  public static void checkAllNull(String message, Object... instances) {
    for (int i = 0; i < instances.length; i++) {
      if (instances[i] != null) {
        throw new IllegalArgumentException(message);
      }
    }
  }

  public static Matcher checkMatches(String s, Pattern pattern, String message) {
    Matcher matcher = pattern.matcher(s);
    if (!matcher.matches()) {
      throw new IllegalArgumentException(message);
    }
    return matcher;
  }

  public static String checkNotBlank(String s) {
    return checkNotBlank(s, "blank");
  }

  public static String checkNotBlank(String s, String message) {
    if (s == null || s.isBlank()) {
      throw new IllegalArgumentException(message);
    }
    return s;
  }

  public static void checkTrue(boolean bool, String message) {
    if (!bool) {
      throw new IllegalArgumentException(message);
    }
  }

  public static File checkFile(File f, Predicate<File> predicate, String message) {
    if (!predicate.test(f)) {
      throw new IllegalArgumentException(message);
    }
    return f;
  }

}
