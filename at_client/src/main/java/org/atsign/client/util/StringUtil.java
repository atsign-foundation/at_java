package org.atsign.client.util;

// enables removal of commons-lang
// TODO: review after JDK upgrade
public class StringUtil {

  public static boolean isBlank(String s) {
    return s == null || s.trim().isEmpty();
  }

  public static boolean isNumeric(String s) {
    return s.matches("\\d+");
  }

}
