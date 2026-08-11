package org.atsign.client.impl.cli;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.nullValue;

import org.junit.jupiter.api.Test;
import org.atsign.client.api.AtSign;

class AtSignConverterTest {

  @Test
  void testConvertReturnsExpectedValues() {
    assertThat(new AtSignConverter().convert("alice"), equalTo(AtSign.of("@alice")));
    assertThat(new AtSignConverter().convert("@alice"), equalTo(AtSign.of("@alice")));
    assertThat(new AtSignConverter().convert(null), nullValue());
    assertThat(new AtSignConverter().convert(""), nullValue());
    assertThat(new AtSignConverter().convert(" "), nullValue());
  }
}
