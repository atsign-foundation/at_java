package org.atsign.client.api;

import org.atsign.common.AtSign;

/**
 * Constants and Utility methods for "well known" standard keys
 */
public class AtKeyNames {

  /**
   * Key name for an Atsign's public encryption key
   */
  public static final String PUBLIC_ENCRYPT = "publickey";

  /**
   * Key name used during activation of Atsign server with CRAM authentication
   */
  public static final String PRIVATE_AT_SECRET = "privatekey:at_secret";

  /**
   * Key name used to store a shared encryption key between two {@link AtSign}s
   */
  public static final String SHARED_KEY = "shared_key";

  /**
   * Key name used during enrollment completion, this is used to obtain the self encryption
   * for an {@link AtSign}
   */
  public static final String SELF_ENCRYPTION_KEY = "default_self_enc_key";

  /**
   * Key name used during enrollment completion, this is used to obtain the private encryption
   * key for an {@link AtSign}
   */
  public static final String ENCRYPT_PRIVATE_KEY = "default_enc_private_key";

  /**
   * Returns the key name used to store the shared encryption key encrypted with
   * the sharedBy {@link AtSign}'s public key
   *
   * @param sharedWith the sharedWith (recipient) {@link AtSign}
   * @return the fully qualified key name
   */
  public static String toSharedByMeKeyName(AtSign sharedWith) {
    return String.format("%s.%s", SHARED_KEY, sharedWith.withoutPrefix());
  }


  /**
   *
   * @return the key used to shared
   */
  public static String toSharedWithMeKeyName(AtSign sharedBy, AtSign sharedWith) {
    return String.format("%s:%s%s", SHARED_KEY, sharedWith, sharedBy);
  }

}
