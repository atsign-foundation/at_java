package org.atsign.client.impl.commands;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Map;

import org.atsign.client.api.Metadata;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;

public class DataResponsesTest {

  @Test
  public void testMatchDataSuccess() {
    assertThat(DataResponses.matchDataSuccess("data:success"), is("success"));
  }

  @Test
  public void testMatchDataSuccessThrowException() {
    Exception ex = assertThrows(Exception.class, () -> DataResponses.matchDataSuccess("error:epicfail"));
    assertThat(ex.getMessage(), containsString("expected [data:(success)] but input was : error:epicfail"));
  }

  @Test
  public void testMatchDataStringNoWhitespace() {
    assertThat(DataResponses.matchDataStringNoWhitespace("data:somestring"), is("somestring"));
  }

  @Test
  public void testMatchDataStringNoWhitespaceThrowException() {
    assertThrows(Exception.class, () -> DataResponses.matchDataStringNoWhitespace("data:some string with spaces"));
  }

  @Test
  public void testMatchData() {
    assertThat(DataResponses.matchData("data:some string"), is("some string"));
    assertThat(DataResponses.matchData("data:{\"member\":value}"), is("{\"member\":value}"));
  }

  @Test
  public void testMatchDataInt() {
    assertThat(DataResponses.matchDataInt("data:42"), is(42));
  }

  @Test
  public void testMatchDataNegativeInt() {
    assertThat(DataResponses.matchDataInt("data:-42"), is(-42));
  }

  @Test
  public void testMatchDataJsonList() {
    assertThat(DataResponses.matchDataJsonList("data:[1,2,3]"), contains(1, 2, 3));
  }

  @Test
  public void testMatchDataJsonListOfStrings() {
    assertThat(DataResponses.matchDataJsonListOfStrings("data:[\"a\",\"b\"]"), contains("a", "b"));
  }

  @Test
  public void testMatchDataJsonMapOfStringsNonEmpty() {
    assertThat(DataResponses.matchDataJsonMapOfStrings("data:{\"k\":\"v\"}"), hasEntry("k", "v"));
  }

  @Test
  public void testMatchDataJsonMapOfStringsAllowEmpty() {
    Map<String, String> result = DataResponses.matchDataJsonMapOfStrings("data:{}", true);
    assertThat(result.entrySet(), Matchers.empty());
  }

  @Test
  public void testMatchDataJsonMapOfObjectsNonEmpty() {
    Map<String, Object> result = DataResponses.matchDataJsonMapOfObjects("data:{\"k\":42}");
    assertThat(result.get("k"), is(42));
  }

  @Test
  public void testMatchDataJsonMapOfObjectsAllowEmpty() {
    Map<String, Object> result = DataResponses.matchDataJsonMapOfObjects("data:{}", true);
    assertThat(result.isEmpty(), is(true));
  }

  @Test
  public void testMatchLookupResponse() {
    String json = "data:{\"data\":\"xyz\"}";
    LookupResponse result = DataResponses.matchLookupResponse(json);
    assertThat(result.data, is("xyz"));
  }

  @Test
  public void testMatchMetadata() {
    String json = "data:{\"ttr\":0}";
    Metadata result = DataResponses.matchMetadata(json);
    assertThat(result.ttr(), is(0L));
  }
}
