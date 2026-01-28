package org.atsign.cucumber.steps;

import io.cucumber.java.ParameterType;
import org.atsign.common.AtException;
import org.atsign.common.AtSign;

import java.io.File;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ParameterTypes {

  @ParameterType("@\\S+")
  public AtSign atsign(String s) {
    return new AtSign(s);
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
    return (Class<AtException>) Class.forName("org.atsign.common.exceptions." + className);
  }

  @ParameterType("\\w+")
  public TimeUnit timeunit(String unit) {
    String s = unit.toUpperCase();
    if (!s.endsWith("S")) {
      s += "S";
    }
    return TimeUnit.valueOf(s);
  }

}
