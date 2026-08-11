package org.atsign.client.api;

import org.atsign.client.impl.common.TypedString;

/**
 * Identifier that represents a specific {@link AtSign} enrollment
 * and association with a specific key pair used for authentication management (PKAM).
 */
public class EnrollmentId extends TypedString {

  private EnrollmentId(String id) {
    super(id);
  }

  /**
   * @param s the string representation of the enrollment id
   * @return null if param is null or blank, otherwise the corresponding {@link EnrollmentId}
   */
  public static EnrollmentId of(String s) {
    return s != null && !s.trim().isEmpty() ? new EnrollmentId(s) : null;
  }

  /**
   * @param s the string representation of the enrollment id
   * @return null if param is null or blank, otherwise the corresponding {@link EnrollmentId}
   * @deprecated use {@link #of(String)}
   */
  @Deprecated
  public static EnrollmentId createEnrollmentId(String s) {
    return EnrollmentId.of(s);
  }
}
