package org.atsign.client.api;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.Matchers.sameInstance;

import org.junit.jupiter.api.Test;

class KeyIdTest {

  @Test
  void testOfReturnsNullForNullOrBlank() {
    assertThat(KeyId.of(null), nullValue());
    assertThat(KeyId.of(""), nullValue());
    assertThat(KeyId.of("   "), nullValue());
  }

  @Test
  void testOfPreservesTheValue() {
    assertThat(KeyId.of("default").toString(), equalTo("default"));
  }

  @Test
  void testEqualByValue() {
    assertThat(KeyId.of("a"), equalTo(KeyId.of("a")));
    assertThat(KeyId.of("a").hashCode(), equalTo(KeyId.of("a").hashCode()));
    assertThat(KeyId.of("a"), not(equalTo(KeyId.of("b"))));
  }

  @Test
  void testInstancesAreNotShared() {
    assertThat(KeyId.of("a"), not(sameInstance(KeyId.of("a"))));
  }
}
