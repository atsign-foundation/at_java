package org.atsign.cucumber.steps;

import io.cucumber.java.ParameterType;
import org.atsign.common.AtException;
import org.atsign.common.AtSign;

import java.io.File;
import java.util.concurrent.TimeUnit;

public class ParameterTypes {

  @ParameterType(".+")
  public AtSign atsign(String s) {
    return new AtSign(s);
  }

  @ParameterType(".+")
  public File path(String s) {
    return new File(s);
  }

  @ParameterType("AtKeyNotFoundException")
  public Class<AtException> exception(String className) throws ClassNotFoundException {
    return (Class<AtException>) Class.forName("org.atsign.common.exceptions." + className);
  }

  @ParameterType("SECONDS|MINUTES")
  public TimeUnit timeunit(String unit) {
    return TimeUnit.valueOf(unit);
  }

}
