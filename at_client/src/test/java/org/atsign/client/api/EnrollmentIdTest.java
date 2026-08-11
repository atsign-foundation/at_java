package org.atsign.client.api;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;

import org.junit.jupiter.api.Test;

class EnrollmentIdTest {

  @Test
  void testOfReturnsNullForNullOrBlank() {
    assertThat(EnrollmentId.of(null), nullValue());
    assertThat(EnrollmentId.of(""), nullValue());
    assertThat(EnrollmentId.of("   "), nullValue());
  }

  @Test
  void testOfPreservesTheValue() {
    assertThat(EnrollmentId.of("abc123").toString(), equalTo("abc123"));
    assertThat(EnrollmentId.of("abc123"), equalTo(EnrollmentId.of("abc123")));
    assertThat(EnrollmentId.of("abc123"), not(equalTo(EnrollmentId.of("def456"))));
  }

  @Test
  @SuppressWarnings("deprecation")
  void testDeprecatedFactoryDelegatesToOf() {
    assertThat(EnrollmentId.createEnrollmentId("abc123"), equalTo(EnrollmentId.of("abc123")));
    assertThat(EnrollmentId.createEnrollmentId(null), nullValue());
    assertThat(EnrollmentId.createEnrollmentId(""), nullValue());
    assertThat(EnrollmentId.createEnrollmentId("   "), nullValue());
  }
}
