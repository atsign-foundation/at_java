package org.atsign.client.util;

public class StringUtil {

  public static boolean isBlank(String s) {
    return s == null || s.isBlank();
  }

  public static boolean isNumeric(String s) {
    return s.matches("\\d+");
  }

}
