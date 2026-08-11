package org.atsign.client.api;

import org.junit.jupiter.api.Test;

import static org.atsign.client.api.AtKeyNames.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

class AtKeyNamesTest {

  @Test
  void testToSharedByMeKeyNameReturnsExpectedValue() {
    assertThat(toSharedByMeKeyName(AtSign.of("gary")), equalTo("shared_key.gary"));
  }

}
