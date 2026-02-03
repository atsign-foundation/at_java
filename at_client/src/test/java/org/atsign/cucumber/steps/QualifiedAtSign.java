package org.atsign.cucumber.steps;

import org.atsign.client.util.EnrollmentId;
import org.atsign.common.AtSign;

import java.util.Objects;

public class QualifiedAtSign {

    private final AtSign atSign;

    private final EnrollmentId enrollmentId;

    public QualifiedAtSign(AtSign atSign, EnrollmentId enrollmentId) {
        this.atSign = atSign;
        this.enrollmentId = enrollmentId;
    }

    public AtSign getAtSign() {
        return atSign;
    }

    public EnrollmentId getEnrollmentId() {
        return enrollmentId;
    }

    @Override
    public String toString() {
        return enrollmentId != null ? atSign + "(" + enrollmentId + ")" : atSign.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        QualifiedAtSign that = (QualifiedAtSign) o;
        return Objects.equals(atSign, that.atSign) && Objects.equals(enrollmentId, that.enrollmentId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(atSign, enrollmentId);
    }
}
