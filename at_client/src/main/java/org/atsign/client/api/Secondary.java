package org.atsign.client.api;

import org.atsign.common.AtException;
import org.atsign.common.exceptions.*;
import org.atsign.common.AtSign;

import java.io.Closeable;
import java.io.IOException;

/**
 * Clients ultimately talk to a Secondary server - usually this is a microservice which implements
 * the @ protocol server spec, running somewhere in the cloud.
 * <br>
 * In the initial implementation we just have AtClientImpl talking to a RemoteSecondary which in
 * turn
 * talks, via TLS over a secure socket, to the cloud Secondary server.
 * <br>
 * As we implement client-side offline storage, performance caching etc., we can expect e.g.
 * <br/>
 * AtClient -> FastCacheSecondary -> OfflineStorageSecondary -> RemoteSecondary<br/>
 * where FastCacheSecondary might be an in-memory LRU cache, and OfflineStorageSecondary is a
 * persistent cache of some or all of the information in the RemoteSecondary. To make this
 * possible, each Secondary will need to be able to fully handle the @ protocol, thus the
 * interface is effectively the same as when interacting with a cloud secondary via openssl
 * from command line.
 */
public interface Secondary extends AtEvents.AtEventListener, Closeable {
  /**
   * @param command in @ protocol format
   * @param throwExceptionOnErrorResponse sometimes we want to inspect an error response,
   *        sometimes we want to just throw an exception
   * @return response in @ protocol format
   * @throws AtException if there was an error response and throwExceptionOnErrorResponse is true
   * @throws IOException if one is encountered
   */
  Response executeCommand(String command, boolean throwExceptionOnErrorResponse) throws IOException, AtException;

  void startMonitor();

  void stopMonitor();

  boolean isMonitorRunning();

  class Response {
    private String rawDataResponse = null;
    private String rawErrorResponse;
    private String errorCode;
    private String errorText;

    public String getRawDataResponse() {
      return rawDataResponse;
    }

    public void setRawDataResponse(String s) {
      rawDataResponse = s;
      rawErrorResponse = null;
      errorCode = null;
      errorText = null;
    }

    public String getRawErrorResponse() {
      return rawErrorResponse;
    }

    public void setRawErrorResponse(String s) {
      // In format "AT1234-meaning of error code : <any other text>"
      rawErrorResponse = s;
      rawDataResponse = null;

      int codeDelimiter = rawErrorResponse.indexOf(":");
      String errorCodeSegment = rawErrorResponse.substring(0, codeDelimiter).trim();
      String[] separatedByHyphen = errorCodeSegment.split("-");
      errorCode = separatedByHyphen[0].trim();
      errorText = rawErrorResponse.substring(codeDelimiter + 1).trim();
    }

    public boolean isError() {
      return rawErrorResponse != null;
    }

    public String getErrorCode() {
      return errorCode;
    }

    public String getErrorText() {
      return errorText;
    }

    @Override
    public String toString() {
      if (isError()) {
        return "error:" + rawErrorResponse;
      } else {
        return "data:" + rawDataResponse;
      }
    }

    public AtException getException() {
      if (!isError()) {
        return null;
      }
      if ("AT0001".equals(errorCode)) {
        return new AtServerRuntimeException(errorText);
      } else if ("AT0003".equals(errorCode)) {
        return new AtInvalidSyntaxException(errorText);
      } else if ("AT0005".equals(errorCode)) {
        return new AtBufferOverFlowException(errorText);
      } else if ("AT0006".equals(errorCode)) {
        return new AtOutboundConnectionLimitException(errorText);
      } else if ("AT0007".equals(errorCode)) {
        return new AtSecondaryNotFoundException(errorText);
      } else if ("AT0008".equals(errorCode)) {
        return new AtHandShakeException(errorText);
      } else if ("AT0009".equals(errorCode)) {
        return new AtUnauthorizedException(errorText);
      } else if ("AT0010".equals(errorCode)) {
        return new AtInternalServerError(errorText);
      } else if ("AT0011".equals(errorCode)) {
        return new AtInternalServerException(errorText);
      } else if ("AT0012".equals(errorCode)) {
        return new AtInboundConnectionLimitException(errorText);
      } else if ("AT0013".equals(errorCode)) {
        return new AtBlockedConnectionException(errorText);
      } else if ("AT0015".equals(errorCode)) {
        return new AtKeyNotFoundException(errorText);
      } else if ("AT0016".equals(errorCode)) {
        return new AtInvalidAtKeyException(errorText);
      } else if ("AT0021".equals(errorCode)) {
        return new AtSecondaryConnectException(errorText);
      } else if ("AT0022".equals(errorCode)) {
        return new AtIllegalArgumentException(errorText);
      } else if ("AT0023".equals(errorCode)) {
        return new AtTimeoutException(errorText);
      } else if ("AT0024".equals(errorCode)) {
        return new AtServerIsPausedException(errorText);
      } else if ("AT0401".equals(errorCode)) {
        return new AtUnauthenticatedException(errorText);
      }

      return new AtNewErrorCodeWhoDisException(errorCode, errorText);
    }
  }

  class Address {
    public final String host;
    public final int port;

    public Address(String host, int port) {
      this.host = host;
      this.port = port;
    }

    public static Address fromString(String hostAndPort) throws IllegalArgumentException {
      String[] split = hostAndPort.split(":");
      if (split.length != 2) {
        throw new IllegalArgumentException(
            "Cannot construct Secondary.Address from malformed host:port string '" + hostAndPort + "'");
      }
      String host = split[0];
      int port;
      try {
        port = Integer.parseInt(split[1]);
      } catch (NumberFormatException e) {
        throw new IllegalArgumentException(
            "Cannot construct Secondary.Address from malformed host:port string '" + hostAndPort + "'");
      }
      return new Address(host, port);
    }

    @Override
    public String toString() {
      return host + ":" + port;
    }
  }

  interface AddressFinder {
    Address findSecondary(AtSign atSign) throws IOException, AtSecondaryNotFoundException;
  }
}
