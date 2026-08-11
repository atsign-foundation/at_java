package org.atsign.client.api;

import org.atsign.client.impl.common.Preconditions;
import org.atsign.client.impl.common.TypedString;


/**
 * The identity of a person or system in the Atsign Platform
 */
public class AtSign extends TypedString {

  private AtSign(String s) {
    super(formatAtSign(s));
  }

  public String withoutPrefix() {
    return toString().substring(1);
  }

  /**
   * A factory method for instantiating {@link AtSign} instances.
   *
   * @param s the string representation of the Atsign (can be with our without @ prefix)
   * @return null if param is null or blank, otherwise the corresponding {@link AtSign} for the param
   */
  public static AtSign of(String s) {
    return s != null && !s.isBlank() ? new AtSign(s) : null;
  }

  /**
   * @param s the string representation of the Atsign (can be with our without @ prefix)
   * @return null if param is null or blank, otherwise the corresponding {@link AtSign} for the param
   * @deprecated use {@link #of(String)}
   */
  @Deprecated
  public static AtSign createAtSign(String s) {
    return AtSign.of(s);
  }

  /**
   * Returns a formatted atSign, ensuring that there is an @ prefix
   *
   * @param s prefix or unprefixed atsign
   * @return prefixed atsign
   */
  public static String formatAtSign(String s) {
    String result = Preconditions.checkNotNull(s).trim();
    return result.startsWith("@") ? result : "@" + s;
  }
}
