package org.atsign.client.impl.commands;

import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import org.atsign.client.api.Metadata;
import org.atsign.client.impl.util.JsonUtils;

/**
 * Utilities for handling data responses in the At Protocol.
 */
public class DataResponses {

  /**
   * models server response string which is non-empty JSON map
   */
  private static final Pattern DATA_JSON_NON_EMPTY_MAP = Pattern.compile("data:(\\{.+})");

  /**
   * models server response string which is JSON map
   */
  private static final Pattern DATA_JSON_MAP = Pattern.compile("data:(\\{.*})");

  /**
   * models server response string which is non-empty JSON list
   */
  private static final Pattern DATA_JSON_LIST = Pattern.compile("data:(\\[.*])");

  /**
   * models server response string which is integer
   */
  private static final Pattern DATA_INT = Pattern.compile("data:(-?\\d+)");

  /**
   * models server response string containing no whitespace
   */
  private static final Pattern DATA_NON_WHITESPACE = Pattern.compile("data:(\\S+)");

  /**
   * models server data response
   */
  private static final Pattern DATA = Pattern.compile("data:(.+)");

  /**
   * models server response string containing no whitespace
   */
  private static final Pattern DATA_SUCCESS = Pattern.compile("data:(success)");

  /**
   * Use this to verify a "data:success" response.
   *
   * @param input The At Server response to verify.
   * @return "success" if this matches.
   */
  public static String matchDataSuccess(String input) {
    return Responses.match(input, DATA_SUCCESS);
  }

  /**
   * Use this to verify a "data:xxxxx" response (no whitespace in the value).
   *
   * @param input The At Server response to verify.
   * @return The value after the "data:" prefix.
   */
  public static String matchDataStringNoWhitespace(String input) {
    return Responses.match(input, DATA_NON_WHITESPACE);
  }

  /**
   * Use this to verify a "data:x xx xx" response (whitespace permitted).
   *
   * @param input The At Server response to verify.
   * @return The value after the "data:" prefix.
   */
  public static String matchData(String input) {
    return Responses.match(input, DATA);
  }

  /**
   * Use this to verify a "data:123" response (negative numbers permitted).
   *
   * @param input The At Server response to verify.
   * @return The value after the "data:" prefix.
   */
  public static int matchDataInt(String input) {
    return Responses.match(input, DATA_INT, Integer::parseInt);
  }

  /**
   * Use this to verify a "data:[...]" response where the value is a JSON encoded list.
   *
   * @param input The At Server response to verify.
   * @return The {@link List} after the "data:" prefix.
   */
  public static List<Object> matchDataJsonList(String input) {
    return Responses.match(input, DATA_JSON_LIST, Responses::decodeJsonList);
  }

  /**
   * Use this to verify a "data:[...]" response where the value is a JSON encoded list of Strings.
   *
   * @param input The At Server response to verify.
   * @return The {@link List} after the "data:" prefix.
   */
  public static List<String> matchDataJsonListOfStrings(String input) {
    return Responses.match(input, DATA_JSON_LIST, Responses::decodeJsonListOfStrings);
  }

  /**
   * Use this to verify a "data:{...}" response where the value is a JSON encoded map of Strings.
   *
   * @param input The At Server response to verify.
   * @param allowEmpty If true then empty map is permitted.
   * @return The {@link Map} after the "data:" prefix.
   */
  public static Map<String, String> matchDataJsonMapOfStrings(String input, boolean allowEmpty) {
    return Responses.match(input, allowEmpty ? DATA_JSON_MAP : DATA_JSON_NON_EMPTY_MAP,
                           Responses::decodeJsonMapOfStrings);
  }

  /**
   * Use this to verify a "data:{...}" response where the value is a JSON encoded map of objects.
   *
   * @param input The At Server response to verify.
   * @param allowEmpty If true then empty map is permitted.
   * @return The {@link Map} after the "data:" prefix.
   */
  public static Map<String, Object> matchDataJsonMapOfObjects(String input, boolean allowEmpty) {
    return Responses.match(input, allowEmpty ? DATA_JSON_MAP : DATA_JSON_NON_EMPTY_MAP,
                           Responses::decodeJsonMapOfObjects);
  }

  /**
   * Use this to verify a "data:{...}" response where the value is a JSON encoded map of Strings.
   *
   * @param input The At Server response to verify.
   * @return The {@link Map} after the "data:" prefix.
   */
  public static Map<String, String> matchDataJsonMapOfStrings(String input) {
    return matchDataJsonMapOfStrings(input, false);
  }

  /**
   * Use this to verify a "data:{...}" response where the value is a JSON encoded map of objects.
   *
   * @param input The At Server response to verify.
   * @return The {@link Map} after the "data:" prefix.
   */
  public static Map<String, Object> matchDataJsonMapOfObjects(String input) {
    return matchDataJsonMapOfObjects(input, false);
  }

  /**
   * Use this to verify a "data:{...}" response where the value is a JSON encoded
   * {@link LookupResponse}
   *
   * @param input The At Server response to verify.
   * @return The {@link LookupResponse} after the "data:" prefix.
   */
  public static LookupResponse matchLookupResponse(String input) {
    return Responses.match(input, DATA_JSON_NON_EMPTY_MAP, DataResponses::toLookupResponse);
  }

  private static LookupResponse toLookupResponse(String input) {
    return JsonUtils.readValue(input, LookupResponse.class);
  }

  /**
   * Use this to verify a "data:{...}" response where the value is a JSON encoded
   * {@link Metadata}
   *
   * @param input The At Server response to verify.
   * @return The {@link Metadata} after the "data:" prefix.
   */
  public static Metadata matchMetadata(String input) {
    return Responses.match(input, DATA_JSON_NON_EMPTY_MAP, DataResponses::toMetaData);
  }

  private static Metadata toMetaData(String input) {
    return JsonUtils.readValue(input, Metadata.class);
  }
}
