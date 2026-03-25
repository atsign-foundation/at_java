package org.atsign.client.impl.commands;

import static org.atsign.client.impl.commands.AtExceptions.toTypedException;
import static org.atsign.client.impl.common.Preconditions.checkNotNull;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.atsign.client.impl.exceptions.AtException;

/**
 * Utility methods for handling error responses in the At Protocol.
 */
public class ErrorResponses {

  /**
   * Models server data response
   */
  private static final Pattern ERROR = Pattern.compile("error:(.+)");

  private static final Pattern ERROR_WITH_CODE = Pattern.compile("error:(AT\\d+)([^:]*):\\s*(.+)");

  private static final Pattern ERROR_WITH_JSON = Pattern.compile("error:(\\{.+})");

  /**
   * Use this to verify a "error:xxxx" response.
   *
   * @param response A response string from an AtSign command interface
   * @return the response with the "error:" prefix removed
   */
  public static String matchError(String response) {
    return Responses.match(response, ERROR);
  }

  /**
   * Utility method that can be used to wrap a response and throw a typed
   * {@link AtException} is the response represents an error.
   *
   * @param response A response string from an AtSign command interface
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

  private static AtException getAtExceptionIfError(String response) throws AtException {
    checkNotNull(response);
    Matcher matcher = ERROR_WITH_JSON.matcher(response);
    if (matcher.matches()) {
      Map<String, Object> map = Responses.decodeJsonMapOfObjects(matcher.group(1));
      return toTypedException((String) map.get("errorCode"), (String) map.get("errorDescription"));
    }
    matcher = ERROR_WITH_CODE.matcher(response);
    if (matcher.matches()) {
      return toTypedException(matcher.group(1), matcher.group(3));
    }
    matcher = ERROR.matcher(response);
    if (matcher.matches()) {
      return new AtException(matcher.group(1));
    }
    return null;
  }

}
