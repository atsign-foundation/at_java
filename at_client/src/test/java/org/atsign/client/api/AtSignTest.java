package org.atsign.client.api;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.nullValue;

import org.junit.jupiter.api.Test;

class AtSignTest {

  @Test
  void testStaticCreateMethod() {
    assertThat(AtSign.of("@fred"), equalTo(AtSign.of("@fred")));
    assertThat(AtSign.of("fred"), equalTo(AtSign.of("@fred")));
    assertThat(AtSign.of(null), nullValue());
    assertThat(AtSign.of(""), nullValue());
  }

  @Test
  void testWithoutPrefixReturnsExpectedResult() {
    assertThat(AtSign.of("fred").withoutPrefix(), equalTo("fred"));
    assertThat(AtSign.of("@fred").withoutPrefix(), equalTo("fred"));
  }

  @Test
  @SuppressWarnings("deprecation")
  void testDeprecatedFactoryDelegatesToOf() {
    assertThat(AtSign.createAtSign("@fred"), equalTo(AtSign.of("@fred")));
    assertThat(AtSign.createAtSign("fred"), equalTo(AtSign.of("@fred")));
    assertThat(AtSign.createAtSign(null), nullValue());
    assertThat(AtSign.createAtSign(""), nullValue());
    assertThat(AtSign.createAtSign("   "), nullValue());
  }

  @Test
  void testFormatAtSignReturnsExpectedResults() {
    assertThat(AtSign.formatAtSign("@fred"), equalTo("@fred"));
    assertThat(AtSign.formatAtSign("fred"), equalTo("@fred"));
    assertThat(AtSign.formatAtSign("@fred "), equalTo("@fred"));
  }
}
