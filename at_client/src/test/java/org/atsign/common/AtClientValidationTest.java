package org.atsign.common;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.atsign.client.util.AtClientValidation;
import org.atsign.common.exceptions.AtInvalidAtKeyException;
import org.junit.jupiter.api.Test;

public class AtClientValidationTest {

  // valid key names (like "test", but not "cached:public:test@bob" <-- these key
  // names will always fail this test)
  @Test
  public void validateKeyNameTest() {
    // null key name
    AtException ex = assertThrows(AtInvalidAtKeyException.class, () -> {
      String keyName = null;
      AtClientValidation.validateKeyName(keyName);
    });
    assertThat(ex.getMessage(), equalTo("Key cannot be null or empty"));

    // empty key name
    ex = assertThrows(AtInvalidAtKeyException.class, () -> {
      String keyName = "";
      AtClientValidation.validateKeyName(keyName);
    });
    assertThat(ex.getMessage(), equalTo("Key cannot be null or empty"));

    // key name with @
    ex = assertThrows(AtInvalidAtKeyException.class, () -> {
      String keyName = "test@bob";
      AtClientValidation.validateKeyName(keyName);
    });
    assertThat(ex.getMessage(), equalTo("Key cannot contain @"));

    // key name with spaces
    ex = assertThrows(AtInvalidAtKeyException.class, () -> {
      String keyName = " te st ";
      AtClientValidation.validateKeyName(keyName);
    });
    assertThat(ex.getMessage(), equalTo("Key cannot have spaces"));
  }

  // valid metadata (null check, ttl, ttb, ttr)
  @Test
  public void validateMetadataTest() {

    // null metadata
    AtException ex = assertThrows(AtInvalidAtKeyException.class, () -> {
      Metadata metadata = null;
      AtClientValidation.validateMetadata(metadata);
    });
    assertThat(ex.getMessage(), equalTo("Metadata cannot be null"));

    // null ttl
    ex = assertThrows(AtInvalidAtKeyException.class, () -> {
      Metadata metadata = new Metadata();
      metadata.ttl = null;
      metadata.ttb = 0;
      metadata.ttr = -1;
      AtClientValidation.validateMetadata(metadata);
    });
    assertThat(ex.getMessage(), equalTo("ttl cannot be null and cannot be negative"));

    // negative ttl
    ex = assertThrows(AtInvalidAtKeyException.class, () -> {
      Metadata metadata = new Metadata();
      metadata.ttl = -100;
      metadata.ttb = 0;
      metadata.ttr = -1;
      AtClientValidation.validateMetadata(metadata);
    });
    assertThat(ex.getMessage(), equalTo("ttl cannot be null and cannot be negative"));

    // null ttb
    ex = assertThrows(AtInvalidAtKeyException.class, () -> {
      Metadata metadata = new Metadata();
      metadata.ttl = 0;
      metadata.ttb = null;
      metadata.ttr = -1;
      AtClientValidation.validateMetadata(metadata);
    });
    assertThat(ex.getMessage(), equalTo("ttb cannot be null and cannot be negative"));

    // negative ttb
    ex = assertThrows(AtInvalidAtKeyException.class, () -> {
      Metadata metadata = new Metadata();
      metadata.ttl = 0;
      metadata.ttb = -100;
      metadata.ttr = -1;
      AtClientValidation.validateMetadata(metadata);
    });
    assertThat(ex.getMessage(), equalTo("ttb cannot be null and cannot be negative"));

    // null ttr
    ex = assertThrows(AtInvalidAtKeyException.class, () -> {
      Metadata metadata = new Metadata();
      metadata.ttl = 0;
      metadata.ttb = 0;
      metadata.ttr = null;
      AtClientValidation.validateMetadata(metadata);
    });
    assertThat(ex.getMessage(), equalTo("ttr cannot be null and cannot be < -1"));

    // ttr < -1
    ex = assertThrows(AtInvalidAtKeyException.class, () -> {
      Metadata metadata = new Metadata();
      metadata.ttl = 0;
      metadata.ttb = 0;
      metadata.ttr = -2;
      AtClientValidation.validateMetadata(metadata);
    });
    assertThat(ex.getMessage(), equalTo("ttr cannot be null and cannot be < -1"));

  }
}
