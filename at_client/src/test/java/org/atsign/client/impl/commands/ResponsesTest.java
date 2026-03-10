package org.atsign.client.impl.commands;

import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import org.junit.jupiter.api.Test;

class ResponsesTest {

  @Test
  void testDecodeJsonMapOfStringsReturnsExpectedResults() {
    String json = "{\"key\":\"public:test@gary\",\"data\":\"hello world\"}";

    Map<String, String> expected = new HashMap<>();
    expected.put("key", "public:test@gary");
    expected.put("data", "hello world");

    assertThat(Responses.decodeJsonMapOfStrings(json), equalTo(expected));
    assertThat(Responses.decodeJsonMapOfStrings("{}"), equalTo(emptyMap()));
    assertThrows(RuntimeException.class, () -> Responses.decodeJsonMapOfStrings("x" + json + "x"));
  }

  @Test
  void testDecodeJsonMapOfObjectsReturnsExpectedResults() {
    String json = "{\"key\":\"public:test@gary\",\"ttl\":1000001,\"ccd\":true}";

    Map<String, Object> expected = new HashMap<>();
    expected.put("key", "public:test@gary");
    expected.put("ttl", 1000001);
    expected.put("ccd", true);

    assertThat(Responses.decodeJsonMapOfObjects(json), equalTo(expected));
    assertThat(Responses.decodeJsonMapOfObjects("{}"), equalTo(emptyMap()));
    assertThrows(RuntimeException.class, () -> Responses.decodeJsonMapOfObjects("x" + json + "x"));
  }

  @Test
  void testDecodeJsonListReturnsExpectedResults() {
    String json = "[\"public:test@gary\",\"public:test2@gary\"]";

    List<Object> expected = new ArrayList<>();
    expected.add("public:test@gary");
    expected.add("public:test2@gary");

    assertThat(Responses.decodeJsonList(json), equalTo(expected));
    assertThat(Responses.decodeJsonList("[]"), equalTo(emptyList()));
    assertThrows(RuntimeException.class, () -> Responses.decodeJsonList("x" + json + "x"));
  }

  @Test
  void decodeJsonListOfStringsReturnsExpectedResults() {
    String json = "[\"public:test@gary\",\"public:test2@gary\"]";

    List<String> expected = new ArrayList<>();
    expected.add("public:test@gary");
    expected.add("public:test2@gary");

    assertThat(Responses.decodeJsonListOfStrings(json), equalTo(expected));
    assertThat(Responses.decodeJsonListOfStrings("[]"), equalTo(emptyList()));
    assertThrows(RuntimeException.class, () -> Responses.decodeJsonListOfStrings("x" + json + "x"));
  }

  @Test
  void testMatchReturnsExpectedResults() {
    assertThat(Responses.match("abc", Pattern.compile("abc")), equalTo("abc"));
    assertThat(Responses.match("abc", Pattern.compile("a(bc)")), equalTo("bc"));
    assertThat(Responses.match("abc", Pattern.compile("(a)b(c)")), equalTo("ac"));
    RuntimeException ex = assertThrows(RuntimeException.class, () -> Responses.match("xyz", Pattern.compile("abc")));
    assertThat(ex.getMessage(), containsString("expected [abc] but input was : xyz"));
  }

  @Test
  void testMatchWithTransformReturnsExpectedResults() {
    assertThat(Responses.match("abc", Pattern.compile("abc"), String::toUpperCase), equalTo("ABC"));
    assertThat(Responses.match("abc", Pattern.compile("a(bc)"), String::toUpperCase), equalTo("BC"));
    assertThat(Responses.match("abc", Pattern.compile("(a)b(c)"), String::toUpperCase), equalTo("AC"));
    RuntimeException ex =
        assertThrows(RuntimeException.class, () -> Responses.match("xyz", Pattern.compile("abc"), String::toUpperCase));
    assertThat(ex.getMessage(), containsString("expected [abc] but input was : xyz"));
    ex = assertThrows(RuntimeException.class,
                      () -> Responses.match("abc", Pattern.compile("abc"), s -> {
                        throw new RuntimeException("deliberate");
                      }));
    assertThat(ex.getMessage(), containsString("deliberate"));
  }
}
