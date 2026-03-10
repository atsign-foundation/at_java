package org.atsign.client.impl.util;

import lombok.extern.slf4j.Slf4j;

import java.nio.charset.StandardCharsets;

/**
 * Utility class with "no throw" methods for converting between byte arrays and Strings
 */
@Slf4j
public class ByteUtils {
  public static String convert(byte[] data) {
    try {
      return new String(data, StandardCharsets.UTF_8);
    } catch (Exception e) {
      log.error("Error occurred while parsing the data to string", e);
      return null;
    }
  }

  public static byte[] convert(String data) {
    try {
      return data.getBytes(StandardCharsets.UTF_8);
    } catch (Exception e) {
      log.error("Error occurred while parsing the string to byte array data", e);
      return null;
    }
  }
}
