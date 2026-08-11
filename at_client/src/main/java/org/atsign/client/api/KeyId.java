package org.atsign.client.api;

import org.atsign.client.impl.common.TypedString;

/**
 * Identifies one cryptographic key. Every piece of material belonging to that key — the public and
 * private halves of a keypair, say — shares a single {@code KeyId}.
 */
public final class KeyId extends TypedString {

  private KeyId(String id) {
    super(id);
  }

  public static KeyId of(String id) {
    return isBlank(id) ? null : new KeyId(id);
  }
}
