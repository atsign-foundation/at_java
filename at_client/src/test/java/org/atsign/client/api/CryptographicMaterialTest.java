package org.atsign.client.api;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.Matchers.sameInstance;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.util.List;

import org.atsign.client.api.CryptographicMaterial.Algorithm;
import org.atsign.client.api.CryptographicMaterial.BytesAsBase64;
import org.atsign.client.api.CryptographicMaterial.Operation;
import org.atsign.client.api.CryptographicMaterial.Role;
import org.atsign.client.api.CryptographicMaterial.Status;
import org.junit.jupiter.api.Test;

class CryptographicMaterialTest {

  private static final OffsetDateTime CREATED_AT = OffsetDateTime.parse("2024-01-01T00:00:00Z");

  private static CryptographicMaterial.CryptographicMaterialBuilder material() {
    return CryptographicMaterial.builder()
        .keyId(KeyId.of("default"))
        .role(Role.publicEncryption)
        .algorithm(Algorithm.rsa2048)
        .bytes(BytesAsBase64.of("c2VjcmV0"))
        .createdAt(CREATED_AT);
  }

  @Test
  void testBytesReturnsNullForNullOrBlank() {
    assertThat(BytesAsBase64.of(null), nullValue());
    assertThat(BytesAsBase64.of(""), nullValue());
    assertThat(BytesAsBase64.of("   "), nullValue());
  }

  @Test
  void testBytesRejectsMalformedBase64() {
    assertThrows(IllegalArgumentException.class, () -> BytesAsBase64.of("not!base64"));
  }

  @Test
  void testBytesExposesBothRepresentations() {
    BytesAsBase64 subject = BytesAsBase64.of("c2VjcmV0");
    assertThat(subject.base64(), equalTo("c2VjcmV0"));
    assertThat(subject.bytes(), equalTo("secret".getBytes(StandardCharsets.UTF_8)));
  }

  @Test
  void testBytesToStringRedacts() {
    assertThat(BytesAsBase64.of("c2VjcmV0").toString(), equalTo("base64(8 chars)"));
    assertThat(BytesAsBase64.of("c2VjcmV0").toString(), not(containsString("c2VjcmV0")));
  }

  @Test
  void testBytesEqualByValue() {
    assertThat(BytesAsBase64.of("c2VjcmV0"), equalTo(BytesAsBase64.of("c2VjcmV0")));
    assertThat(BytesAsBase64.of("c2VjcmV0").hashCode(), equalTo(BytesAsBase64.of("c2VjcmV0").hashCode()));
    assertThat(BytesAsBase64.of("c2VjcmV0"), not(equalTo(BytesAsBase64.of("cHVibGlj"))));
  }

  @Test
  void testTokensAreInterned() {
    assertThat(Role.of("publicEncryption"), sameInstance(Role.publicEncryption));
    assertThat(Algorithm.of("rsa2048"), sameInstance(Algorithm.rsa2048));
    assertThat(Operation.of("sign"), sameInstance(Operation.sign));
    assertThat(Role.of("madeUp"), sameInstance(Role.of("madeUp")));
  }

  @Test
  void testTokenFactoriesReturnNullForNullOrBlank() {
    assertThat(Role.of(null), nullValue());
    assertThat(Role.of(""), nullValue());
    assertThat(Role.of("  "), nullValue());
    assertThat(Algorithm.of(null), nullValue());
    assertThat(Operation.of(" "), nullValue());
  }

  @Test
  void testUnrecognisedTokenIsAcceptedAndPreserved() {
    assertThat(Role.of("quantumFluxCapacitor").toString(), equalTo("quantumFluxCapacitor"));
    assertThat(Algorithm.of("mlkem1024").toString(), equalTo("mlkem1024"));
  }

  @Test
  void testDifferentTypedStringSubclassesNeverCompareEqual() {
    assertThat(Role.of("aes256"), not(equalTo((Object) Algorithm.of("aes256"))));
    assertThat(Operation.of("sign"), not(equalTo((Object) Role.of("sign"))));
    assertThat(KeyId.of("x"), not(equalTo((Object) EnrollmentId.of("x"))));
  }

  @Test
  void testStatusNameIsTheWireValue() {
    assertThat(Status.active.name(), equalTo("active"));
    assertThat(Status.valueOf("retired"), equalTo(Status.retired));
    assertThrows(IllegalArgumentException.class, () -> Status.valueOf("Active"));
    assertThrows(IllegalArgumentException.class, () -> Status.valueOf("compromised"));
  }

  @Test
  void testStatusIsOrderedForwardOnly() {
    assertThat(Status.active.ordinal() < Status.retired.ordinal(), equalTo(true));
    assertThat(Status.retired.ordinal() < Status.dead.ordinal(), equalTo(true));
  }

  @Test
  void testDefaultsAreActiveAndNoOperations() {
    CryptographicMaterial subject = material().build();
    assertThat(subject.getStatus(), equalTo(Status.active));
    assertThat(subject.getOperations(), empty());
    assertThat(subject.getEnrollmentId(), nullValue());
  }

  @Test
  void testWithStatusReturnsCopyAndLeavesOriginalUntouched() {
    CryptographicMaterial original = material().build();
    CryptographicMaterial retired = original.withStatus(Status.retired);

    assertThat(retired.getStatus(), equalTo(Status.retired));
    assertThat(original.getStatus(), equalTo(Status.active));
    assertThat(retired, not(sameInstance(original)));
    assertThat(retired.getKeyId(), equalTo(original.getKeyId()));
  }

  @Test
  void testMaterialToStringDoesNotLeakTheKeyMaterial() {
    String printed = material().build().toString();
    assertThat(printed, containsString("keyId"));
    assertThat(printed, containsString("base64(8 chars)"));
    assertThat(printed, not(containsString("c2VjcmV0")));
  }

  @Test
  void testEqualityComparesTheKeyMaterial() {
    CryptographicMaterial left = material().build();
    CryptographicMaterial right = material().build();
    CryptographicMaterial other = material().bytes(BytesAsBase64.of("cHVibGlj")).build();

    assertThat(left, equalTo(right));
    assertThat(left.hashCode(), equalTo(right.hashCode()));
    assertThat(left, not(equalTo(other)));
  }

  @Test
  void testRequiredFieldsAreRejectedWhenAbsent() {
    assertThrows(NullPointerException.class, () -> CryptographicMaterial.builder()
        .role(Role.publicEncryption)
        .algorithm(Algorithm.rsa2048)
        .bytes(BytesAsBase64.of("c2VjcmV0"))
        .createdAt(CREATED_AT)
        .build());
    assertThrows(NullPointerException.class, () -> material().bytes(null).build());
    assertThrows(NullPointerException.class, () -> material().role(null).build());
  }

  @Test
  void testOperationsAreImmutable() {
    CryptographicMaterial subject = material().operation(Operation.sign).build();
    assertThat(subject.getOperations(), contains(Operation.sign));
    assertThrows(UnsupportedOperationException.class,
                 () -> subject.getOperations().add(Operation.decrypt));
  }

  @Test
  void testOperationsAccumulateSoReplacingNeedsAnExplicitClear() {
    CryptographicMaterial subject = material().operation(Operation.sign).build();

    // Lombok's @Singular setter adds rather than replaces, so this appends to the existing entry.
    CryptographicMaterial appended = subject.toBuilder().operations(List.of(Operation.decrypt)).build();
    assertThat(appended.getOperations(), contains(Operation.sign, Operation.decrypt));

    CryptographicMaterial replaced =
        subject.toBuilder().clearOperations().operations(List.of(Operation.decrypt)).build();
    assertThat(replaced.getOperations(), contains(Operation.decrypt));
  }

  @Test
  void testEnrollmentIdIsCarriedWhenPresent() {
    EnrollmentId enrollmentId = EnrollmentId.of("352b78c8-4b6f-4d07-a9cf-5466512ffa44");
    CryptographicMaterial subject = material().enrollmentId(enrollmentId).build();
    assertThat(subject.getEnrollmentId(), equalTo(enrollmentId));
  }
}
