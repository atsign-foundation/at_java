package org.atsign.client.api;

import org.junit.jupiter.api.Test;

import static org.atsign.client.api.AtKeyNames.*;
import static org.atsign.common.AtSign.createAtSign;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

class AtKeyNamesTest {

  @Test
  void testToSharedByMeKeyNameReturnsExpectedValue() {
    assertThat(toSharedByMeKeyName(createAtSign("gary")), equalTo("shared_key.gary"));
  }

}
