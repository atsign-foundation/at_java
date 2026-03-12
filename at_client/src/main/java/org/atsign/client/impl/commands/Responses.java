package org.atsign.client.impl.commands;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.atsign.client.impl.util.JsonUtils;

import com.fasterxml.jackson.core.type.TypeReference;

/**
 * At Protocol utility code that relates to processing responses from an At Server.
 *
 */

public class Responses {

  /**
   * Decodes a JSON string that represents a Map of Strings.
   *
   * @param json A JSON Object string.
   * @return The map of Strings.
   */
  public static Map<String, String> decodeJsonMapOfStrings(String json) {
    return JsonUtils.readValue(json, new TypeReference<>() {});
  }

  /**
   * Decodes a JSON string that represents a Map of Objects.
   *
   * @param json A JSON Object string.
   * @return The Map of Objects.
   */
  public static Map<String, Object> decodeJsonMapOfObjects(String json) {
    return JsonUtils.readValue(json, new TypeReference<>() {});
  }

  /**
   * Decodes a JSON string that represents a List of Objects.
   *
   * @param json A JSON list string.
   * @return The List of Objects.
   */
  public static List<Object> decodeJsonList(String json) {
    return JsonUtils.readValue(json, new TypeReference<>() {});
  }

  /**
   * Decodes a JSON string that represents a List of Strings.
   *
   * @param json A JSON list string.
   * @return The List of Objects.
   */
  public static List<String> decodeJsonListOfStrings(String json) {
    return JsonUtils.readValue(json, new TypeReference<>() {});
  }

  /**
   * Verifies that the input matches a regular expression and returns the matched groups in the
   * pattern.
   *
   * @param input The At Server response.
   * @param pattern The expected regex for a successful command.
   * @return The concatenation of any groups in the pattern, or the whole input if no groups.
   */
  public static String match(String input, Pattern pattern) {
    Matcher matcher = pattern.matcher(input);
    if (!matcher.matches()) {
      throw new RuntimeException("expected [" + pattern + "] but input was : " + input);
    }
    StringBuilder builder = new StringBuilder();
    if (matcher.groupCount() == 0) {
      builder.append(input);
    } else {
      for (int i = 1; i <= matcher.groupCount(); i++) {
        builder.append(matcher.group(i));
      }
    }
    return builder.toString();
  }

  /**
   * Verifies that the input matches a regular expression and returns the matched groups in the
   * pattern with a transformation applied.
   *
   * @param input The At Server response.
   * @param pattern The expected regex for a successful command.
   * @return The transformed concatenation of any groups in the pattern, or the whole input if no
   *         groups.
   */
  public static <T> T match(String input, Pattern pattern, Function<String, T> transformer) {
    return transformer.apply(match(input, pattern));
  }
}
