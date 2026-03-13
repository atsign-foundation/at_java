package org.atsign.client.api;

import static org.atsign.client.api.AtSign.createAtSign;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.nullValue;

import org.junit.jupiter.api.Test;

class AtSignTest {

  @Test
  void testStaticCreateMethod() {
    assertThat(createAtSign("@fred"), equalTo(createAtSign("@fred")));
    assertThat(createAtSign("fred"), equalTo(createAtSign("@fred")));
    assertThat(createAtSign(null), nullValue());
    assertThat(createAtSign(""), nullValue());
  }

  @Test
  void testWithoutPrefixReturnsExpectedResult() {
    assertThat(createAtSign("fred").withoutPrefix(), equalTo("fred"));
    assertThat(createAtSign("@fred").withoutPrefix(), equalTo("fred"));
  }

  @Test
  void testFormatAtSignReturnsExpectedResults() {
    assertThat(AtSign.formatAtSign("@fred"), equalTo("@fred"));
    assertThat(AtSign.formatAtSign("fred"), equalTo("@fred"));
    assertThat(AtSign.formatAtSign("@fred "), equalTo("@fred"));
  }
}
