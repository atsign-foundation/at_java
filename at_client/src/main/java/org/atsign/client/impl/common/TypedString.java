package org.atsign.client.impl.common;

import static org.atsign.client.impl.common.Preconditions.checkNotNull;

/**
 * Base class for simple classes that have String values.
 */
public abstract class TypedString {

  private final String value;

  protected TypedString(String s) {
    this.value = checkNotNull(s);
  }

  protected static boolean isBlank(String s) {
    return s == null || s.isBlank();
  }

  @Override
  public boolean equals(Object o) {
    return o != null
        && o.getClass() == this.getClass()
        && value.equals(((TypedString) o).value);
  }

  @Override
  public int hashCode() {
    return value.hashCode();
  }

  @Override
  public String toString() {
    return value;
  }
}
