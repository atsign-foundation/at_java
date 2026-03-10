package org.atsign.client.api;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.nullValue;

import org.junit.jupiter.api.Test;

class AtSignTest {

  @Test
  void testStaticCreateMethod() {
    assertThat(AtSign.createAtSign("@fred"), equalTo(new AtSign("@fred")));
    assertThat(AtSign.createAtSign("fred"), equalTo(new AtSign("@fred")));
    assertThat(AtSign.createAtSign(null), nullValue());
    assertThat(AtSign.createAtSign(""), nullValue());
  }

  @Test
  void testWithoutPrefixReturnsExpectedResult() {
    assertThat(new AtSign("fred").withoutPrefix(), equalTo("fred"));
    assertThat(new AtSign("@fred").withoutPrefix(), equalTo("fred"));
  }

  @Test
  void testFormatAtSignReturnsExpectedResults() {
    assertThat(AtSign.formatAtSign("@fred"), equalTo("@fred"));
    assertThat(AtSign.formatAtSign("fred"), equalTo("@fred"));
    assertThat(AtSign.formatAtSign("@fred "), equalTo("@fred"));
  }
}
