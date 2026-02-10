package org.atsign.client.util;

/**
 * Identifier that represent a specific {@link org.atsign.common.AtSign} enrollment
 * and association with a specific key pair used for authentication management (PKAM)
 */
public class EnrollmentId extends TypedString {

  private EnrollmentId(String id) {
    super(id);
  }

  public static EnrollmentId createEnrollmentId(String s) {
    return s != null && !s.trim().isEmpty() ? new EnrollmentId(s) : null;
  }
}
