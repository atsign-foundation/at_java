package org.atsign.client.util;

import static org.atsign.client.util.Preconditions.checkNotNull;

public abstract class TypedString {

  private final String value;

  protected TypedString(String s) {
    this.value = checkNotNull(s);
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
