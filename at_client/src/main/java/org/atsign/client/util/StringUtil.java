package org.atsign.client.util;

/**
 * Utility class with static methods for common String operations
 */
public class StringUtil {

  public static boolean isBlank(String s) {
    return s == null || s.isBlank();
  }

  public static boolean isNumeric(String s) {
    return s.matches("\\d+");
  }

}
