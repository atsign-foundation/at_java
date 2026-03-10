package org.atsign.client.impl.commands;

import static org.atsign.client.impl.commands.AtExceptions.toTypedException;
import static org.atsign.client.impl.common.Preconditions.checkNotNull;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.atsign.client.impl.exceptions.AtException;

/**
 * Utility methods for handling error responses in the AtSign protocol
 */
public class ErrorResponses {

  /**
   * models server data response
   */
  public static final Pattern ERROR = Pattern.compile("error:(.+)");

  public static String matchError(String input) {
    return Responses.match(input, ERROR);
  }

  /**
   * Utility method that can be used to wrap a response and throw a typed
   * {@link AtException} is the response represents an error.
   *
   * @param response a response string from an AtSign command interface
   * @return the response parameter if the response is NOT an error
   * @throws AtException a typed exception if the response is an error
   */
  public static String throwExceptionIfError(String response) throws AtException {
    AtException ex = getAtExceptionIfError(response);
    if (ex != null) {
      throw ex;
    }
    return response;
  }

  public static AtException getAtExceptionIfError(String response) throws AtException {
    checkNotNull(response);
    Matcher matcher = Pattern.compile("error:(AT\\d+)([^:]*):\\s*(.+)").matcher(response);
    if (matcher.matches()) {
      return toTypedException(matcher.group(1), matcher.group(3));
    }
    return null;
  }

}
