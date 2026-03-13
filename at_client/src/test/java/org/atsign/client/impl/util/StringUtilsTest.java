package org.atsign.client.impl.util;

import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.*;

class StringUtilsTest {

  @Test
  void testIsBlankReturnsExpectedResult() {
    assertThat(StringUtils.isBlank(""), is(true));
    assertThat(StringUtils.isBlank(null), is(true));
    assertThat(StringUtils.isBlank(" "), is(true));
    assertThat(StringUtils.isBlank("  "), is(true));

    assertThat(StringUtils.isBlank("a"), is(false));
    assertThat(StringUtils.isBlank("abc"), is(false));
    assertThat(StringUtils.isBlank("abc "), is(false));
    assertThat(StringUtils.isBlank(" abc"), is(false));
    assertThat(StringUtils.isBlank(" abc "), is(false));
    assertThat(StringUtils.isBlank(" ab c"), is(false));
  }

  @Test
  void testIsNumericReturnsExpectedResult() {
    assertThat(StringUtils.isNumeric("1"), is(true));
    assertThat(StringUtils.isNumeric("123"), is(true));
    assertThat(StringUtils.isNumeric("-1"), is(true));
    assertThat(StringUtils.isNumeric("-123"), is(true));

    assertThat(StringUtils.isNumeric(""), is(false));
    assertThat(StringUtils.isNumeric(null), is(false));
    assertThat(StringUtils.isNumeric(" "), is(false));
    assertThat(StringUtils.isNumeric("a"), is(false));
  }
}
