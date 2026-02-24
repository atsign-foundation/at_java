package org.atsign.client.connection.protocol;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Map;

import org.atsign.common.Metadata;
import org.atsign.common.response_models.LookupResponse;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;

public class DataTest {

  @Test
  public void testMatchDataSuccess() {
    assertThat(Data.matchDataSuccess("data:success"), is("success"));
  }

  @Test
  public void testMatchDataSuccessThrowException() {
    Exception ex = assertThrows(Exception.class, () -> Data.matchDataSuccess("error:epicfail"));
    assertThat(ex.getMessage(), containsString("expected [data:(success)] but input was : error:epicfail"));
  }

  @Test
  public void testMatchDataStringNoWhitespace() {
    assertThat(Data.matchDataStringNoWhitespace("data:somestring"), is("somestring"));
  }

  @Test
  public void testMatchDataStringNoWhitespaceThrowException() {
    assertThrows(Exception.class, () -> Data.matchDataStringNoWhitespace("data:some string with spaces"));
  }

  @Test
  public void testMatchData() {
    assertThat(Data.matchData("data:some string"), is("some string"));
    assertThat(Data.matchData("data:{\"member\":value}"), is("{\"member\":value}"));
  }

  @Test
  public void testMatchDataInt() {
    assertThat(Data.matchDataInt("data:42"), is(42));
  }

  @Test
  public void testMatchDataNegativeInt() {
    assertThat(Data.matchDataInt("data:-42"), is(-42));
  }

  @Test
  public void testMatchDataJsonList() {
    assertThat(Data.matchDataJsonList("data:[1,2,3]"), contains(1, 2, 3));
  }

  @Test
  public void testMatchDataJsonListOfStrings() {
    assertThat(Data.matchDataJsonListOfStrings("data:[\"a\",\"b\"]"), contains("a", "b"));
  }

  @Test
  public void testMatchDataJsonMapOfStringsNonEmpty() {
    assertThat(Data.matchDataJsonMapOfStrings("data:{\"k\":\"v\"}"), hasEntry("k", "v"));
  }

  @Test
  public void testMatchDataJsonMapOfStringsAllowEmpty() {
    Map<String, String> result = Data.matchDataJsonMapOfStrings("data:{}", true);
    assertThat(result.entrySet(), Matchers.empty());
  }

  @Test
  public void testMatchDataJsonMapOfObjectsNonEmpty() {
    Map<String, Object> result = Data.matchDataJsonMapOfObjects("data:{\"k\":42}");
    assertThat(result.get("k"), is(42));
  }

  @Test
  public void testMatchDataJsonMapOfObjectsAllowEmpty() {
    Map<String, Object> result = Data.matchDataJsonMapOfObjects("data:{}", true);
    assertThat(result.isEmpty(), is(true));
  }

  @Test
  public void testMatchLookupResponse() {
    String json = "data:{\"data\":\"xyz\"}";
    LookupResponse result = Data.matchLookupResponse(json);
    assertThat(result.data, is("xyz"));
  }

  @Test
  public void testMatchMetadata() {
    String json = "data:{\"ttr\":0}";
    Metadata result = Data.matchMetadata(json);
    assertThat(result.ttr(), is(0L));
  }
}
