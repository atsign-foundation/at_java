package org.atsign.client.impl.cli;

import org.atsign.client.api.AtSign;
import picocli.CommandLine;

import static org.atsign.client.api.AtSign.createAtSign;

/**
 * A Picocli converter for {@link AtSign}
 */
public class AtSignConverter implements CommandLine.ITypeConverter<AtSign> {
  @Override
  public AtSign convert(String s) {
    return createAtSign(s);
  }
}
