package org.atsign.cucumber.helpers;

import org.junit.jupiter.api.Assertions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

public class Helpers {

  private static final Logger LOGGER = LoggerFactory.getLogger(Helpers.class);

  public static boolean testContains(List<Map<String, String>> actualMaps,
                                     List<Map<String, String>> expectedMaps,
                                     boolean exactMatch) {
    List<Map<String, String>> actuals = toCanonicalMaps(actualMaps);
    List<Map<String, String>> expecteds = toCanonicalMaps(expectedMaps);
    for (Map<String, String> expected : expecteds) {
      int match = findMatch(actuals, expected);
      if (match >= 0) {
        actuals.remove(match);
      } else {
        return false;
      }
    }
    return !exactMatch || actuals.isEmpty();
  }

  public static void assertContains(List<Map<String, String>> actualMaps,
                                    List<Map<String, String>> expectedMaps,
                                    boolean exactMatch) {
    List<Map<String, String>> actuals = toCanonicalMaps(actualMaps);
    List<Map<String, String>> expecteds = toCanonicalMaps(expectedMaps);
    for (Map<String, String> expected : expecteds) {
      int match = findMatch(actuals, expected);
      if (match >= 0) {
        actuals.remove(match);
      } else {
        Assertions.fail("no match for " + expected);
      }
    }
    if (exactMatch) {
      assertThat(actuals, is(empty()));
    }
  }

  public static int findMatch(List<Map<String, String>> actuals, Map<String, String> expected) {
    for (int i = 0; i < actuals.size(); i++) {
      if (isMatch(actuals.get(i), expected)) {
        return i;
      }
    }
    return -1;
  }

  public static List<Map<String, String>> toCanonicalMaps(List<Map<String, String>> maps) {
    return maps.stream()
        .map(x -> toCanonicalMap(x))
        .collect(Collectors.toCollection(ArrayList::new));
  }

  public static Map<String, String> toCanonicalMap(Map<String, String> map) {
    return map.entrySet().stream()
        .collect(Collectors.toMap(e -> toCanonicalKey(e.getKey()), e -> e.getValue() != null ? e.getValue() : ""));
  }

  public static String toCanonicalKey(String key) {
    return key.toLowerCase().replaceAll("\\s+", "");
  }

  public static String getFirstValue(Map<String, String> map, String... keys) {
    String value = null;
    for (String key : keys) {
      if ((value = map.get(toCanonicalKey(key))) != null) {
        break;
      }
    }
    return value;
  }

  public static boolean isMatch(Map<String, String> actual, Map<String, String> expected) {
    for (Map.Entry<String, String> entry : expected.entrySet()) {
      if (!isMatch(actual.get(entry.getKey()), entry.getValue())) {
        return false;
      }
    }
    return expected.size() > 0;
  }

  public static boolean isMatch(String actual, String expected) {
    if (expected == null || expected.isEmpty()) {
      return true;
    }
    if (actual == null) {
      return false;
    }
    return actual.matches(expected);
  }

  public static void addKeyValues(List<Map<String, String>> expected, String... keyValues) {
    Map<String, String> map = new HashMap<>();
    assertThat(keyValues.length % 2, equalTo(0));
    for (int i = 0; i < keyValues.length; i++) {
      map.put(keyValues[i], keyValues[++i]);
    }
    expected.add(map);
  }

  public static boolean isHostPortReachable(String hostAndPort, long timeoutMillis) {
    try (Socket socket = new Socket()) {
      Matcher matcher = Pattern.compile("([^:]+):(\\d+)").matcher(hostAndPort);
      if (matcher.matches()) {
        String host = matcher.group(1);
        int port = Integer.parseInt(matcher.group(2));
        socket.connect(new InetSocketAddress(host, port), (int) timeoutMillis);
        return true;
      }
    } catch (IOException e) {
    }
    return false;
  }
}
