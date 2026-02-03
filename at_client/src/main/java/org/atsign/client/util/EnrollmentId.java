package org.atsign.client.util;

public class EnrollmentId extends TypedString {

  private EnrollmentId(String id) {
    super(id);
  }

  public static EnrollmentId createEnrollmentId(String s) {
    return s != null && !s.trim().isEmpty() ? new EnrollmentId(s) : null;
  }
}
