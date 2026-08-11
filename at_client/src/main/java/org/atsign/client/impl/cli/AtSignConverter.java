package org.atsign.client.impl.cli;

import org.atsign.client.api.AtSign;
import picocli.CommandLine;


/**
 * A Picocli converter for {@link AtSign}
 */
public class AtSignConverter implements CommandLine.ITypeConverter<AtSign> {
  @Override
  public AtSign convert(String s) {
    return AtSign.of(s);
  }
}
