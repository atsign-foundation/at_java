package org.atsign.client.impl.util;

/**
 * Utility class with static methods for common String operations.
 */
public class StringUtils {

  public static boolean isBlank(String s) {
    return s == null || s.isBlank();
  }

  public static boolean isNumeric(String s) {
    return s != null && s.matches("-?\\d+");
  }

}
