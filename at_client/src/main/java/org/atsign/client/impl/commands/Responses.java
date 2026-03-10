package org.atsign.client.impl.commands;

import com.fasterxml.jackson.core.type.TypeReference;
import org.atsign.client.impl.util.JsonUtils;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Atsign protocol utility code that relates to processing responses from an atserver
 *
 */

public class Responses {

  public static Map<String, String> decodeJsonMapOfStrings(String json) {
    try {
      return JsonUtils.MAPPER.readValue(json, new TypeReference<>() {});
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  public static Map<String, Object> decodeJsonMapOfObjects(String json) {
    try {
      return JsonUtils.MAPPER.readValue(json, new TypeReference<>() {});
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  public static List<Object> decodeJsonList(String json) {
    try {
      return JsonUtils.MAPPER.readValue(json, new TypeReference<>() {});
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  public static List<String> decodeJsonListOfStrings(String json) {
    try {
      return JsonUtils.MAPPER.readValue(json, new TypeReference<>() {});
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

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

  public static <T> T match(String input, Pattern pattern, Function<String, T> transformer) {
    return transformer.apply(match(input, pattern));
  }
}
