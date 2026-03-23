package org.atsign.cucumber.steps;

import java.io.File;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import io.cucumber.datatable.DataTable;
import org.atsign.client.impl.exceptions.AtException;
import org.atsign.client.api.AtSign;

import io.cucumber.java.ParameterType;

import static org.atsign.client.api.AtSign.createAtSign;

public class ParameterTypes {

  @ParameterType("@\\S+")
  public AtSign atsign(String s) {
    return createAtSign(s);
  }

  @ParameterType("\\S+")
  public File path(String s) {
    return new File(s);
  }

  @ParameterType("(\\d+(st|nd|rd|th))")
  public Integer ordinal(String s) {
    Matcher matcher = Pattern.compile("\\d+").matcher(s);
    if (!matcher.find()) {
      throw new IllegalArgumentException("expected 1st or 2nd etc...");
    }
    return Integer.parseInt(matcher.group(0));
  }

  @ParameterType("(AtException|AtKeyNotFoundException|AtUnauthorizedException|AtUnauthenticatedException)")
  public Class<AtException> exception(String className) throws ClassNotFoundException {
    return (Class<AtException>) Class.forName("org.atsign.client.impl.exceptions." + className);
  }

  @ParameterType("\\w+")
  public TimeUnit timeunit(String unit) {
    String s = unit.toUpperCase();
    if (!s.endsWith("S")) {
      s += "S";
    }
    return TimeUnit.valueOf(s);
  }

  public static byte[] toBytes(DataTable table) {
    List<String> cells = table.asLists().stream()
        .flatMap(List::stream)
        .filter(s -> s != null && !s.isEmpty())
        .collect(Collectors.toList());
    Function<String, Byte> transformer = getByteTransformer(cells.get(0));
    byte[] bytes = new byte[cells.size() - 1];
    for (int i = 0; i < bytes.length; i++) {
      bytes[i] = transformer.apply(cells.get(i + 1));
    }
    return bytes;
  }

  private static Function<String, Byte> getByteTransformer(String base) {
    Matcher matcher = Pattern.compile("base\\s*(\\d+)", Pattern.CASE_INSENSITIVE).matcher(base);
    if (matcher.matches()) {
      return s -> (byte) Integer.parseInt(s, Integer.parseInt(matcher.group(1)));
    } else if (base.equalsIgnoreCase("binary")) {
      return s -> (byte) Integer.parseInt(s, 2);
    } else if (base.toLowerCase().startsWith("hex")) {
      return s -> (byte) Integer.parseInt(s, 2);
    } else {
      throw new IllegalArgumentException("expected leading cell to be base e.g. Binary, Hex, Base 2, etc...");
    }
  }


}
