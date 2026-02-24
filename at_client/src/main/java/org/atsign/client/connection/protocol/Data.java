package org.atsign.client.connection.protocol;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.atsign.common.Json;
import org.atsign.common.Metadata;
import org.atsign.common.response_models.LookupResponse;

import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Utilities for handling data responses in the AtSign protocol
 */
public class Data {

  /**
   * models server response string which is non-empty JSON map
   */
  protected static final Pattern DATA_JSON_NON_EMPTY_MAP = Pattern.compile("data:(\\{.+})");

  /**
   * models server response string which is JSON map
   */
  protected static final Pattern DATA_JSON_MAP = Pattern.compile("data:(\\{.*})");

  /**
   * models server response string which is non-empty JSON list
   */
  protected static final Pattern DATA_JSON_LIST = Pattern.compile("data:(\\[.*])");

  /**
   * models server response string which is integer
   */
  protected static final Pattern DATA_INT = Pattern.compile("data:(-?\\d+)");

  /**
   * models server response string containing no whitespace
   */
  public static final Pattern DATA_NON_WHITESPACE = Pattern.compile("data:(\\S+)");

  /**
   * models server data response
   */
  public static final Pattern DATA = Pattern.compile("data:(.+)");

  /**
   * models server response string containing no whitespace
   */
  public static final Pattern DATA_SUCCESS = Pattern.compile("data:(success)");

  public static String matchDataSuccess(String input) {
    return Responses.match(input, DATA_SUCCESS);
  }

  public static String matchDataStringNoWhitespace(String input) {
    return Responses.match(input, DATA_NON_WHITESPACE);
  }

  public static String matchData(String input) {
    return Responses.match(input, DATA);
  }

  public static int matchDataInt(String input) {
    return Responses.match(input, DATA_INT, Integer::parseInt);
  }

  public static List<Object> matchDataJsonList(String input) {
    return Responses.match(input, DATA_JSON_LIST, Responses::decodeJsonList);
  }

  public static List<String> matchDataJsonListOfStrings(String input) {
    return Responses.match(input, DATA_JSON_LIST, Responses::decodeJsonListOfStrings);
  }

  public static Map<String, String> matchDataJsonMapOfStrings(String input, boolean allowEmpty) {
    return Responses.match(input, allowEmpty ? DATA_JSON_MAP : DATA_JSON_NON_EMPTY_MAP,
                           Responses::decodeJsonMapOfStrings);
  }

  public static Map<String, Object> matchDataJsonMapOfObjects(String input, boolean allowEmpty) {
    return Responses.match(input, allowEmpty ? DATA_JSON_MAP : DATA_JSON_NON_EMPTY_MAP,
                           Responses::decodeJsonMapOfObjects);
  }

  public static Map<String, String> matchDataJsonMapOfStrings(String input) {
    return matchDataJsonMapOfStrings(input, false);
  }

  public static Map<String, Object> matchDataJsonMapOfObjects(String input) {
    return matchDataJsonMapOfObjects(input, false);
  }

  public static LookupResponse matchLookupResponse(String s) {
    return Responses.match(s, DATA_JSON_NON_EMPTY_MAP, Data::toLookupResponse);
  }

  public static LookupResponse toLookupResponse(String s) {
    try {
      return Json.MAPPER.readValue(s, LookupResponse.class);
    } catch (JsonProcessingException e) {
      throw new RuntimeException(e);
    }
  }

  public static Metadata matchMetadata(String s) {
    return Responses.match(s, DATA_JSON_NON_EMPTY_MAP, Data::toMetaData);
  }

  public static Metadata toMetaData(String s) {
    try {
      return Json.MAPPER.readValue(s, Metadata.class);
    } catch (JsonProcessingException e) {
      throw new RuntimeException(e);
    }
  }
}
