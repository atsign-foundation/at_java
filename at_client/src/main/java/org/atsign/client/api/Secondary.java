package org.atsign.client.api;

import java.io.Closeable;
import java.io.IOException;

import org.atsign.common.AtException;
import org.atsign.common.AtSign;
import org.atsign.common.exceptions.*;

/**
 * Clients ultimately talk to a Secondary server - usually this is a microservice which implements
 * the @ protocol server spec, running somewhere in the cloud.
 * <br>
 * In the initial implementation we just have AtClientImpl talking to a RemoteSecondary which in
 * turn
 * talks, via TLS over a secure socket, to the cloud Secondary server.
 * <br>
 * As we implement client-side offline storage, performance caching etc., we can expect e.g.
 * <br>
 * AtClient {@code ->} FastCacheSecondary {@code ->} OfflineStorageSecondary {@code ->}
 * RemoteSecondary
 * <br>
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

  /**
   * Used to hold the partially decoded response from a {@link Secondary}
   */
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
      if (AtServerRuntimeException.CODE.equals(errorCode)) {
        return new AtServerRuntimeException(errorText);
      } else if (AtInvalidSyntaxException.CODE.equals(errorCode)) {
        return new AtInvalidSyntaxException(errorText);
      } else if (AtBufferOverFlowException.CODE.equals(errorCode)) {
        return new AtBufferOverFlowException(errorText);
      } else if (AtOutboundConnectionLimitException.CODE.equals(errorCode)) {
        return new AtOutboundConnectionLimitException(errorText);
      } else if (AtSecondaryNotFoundException.CODE.equals(errorCode)) {
        return new AtSecondaryNotFoundException(errorText);
      } else if (AtHandShakeException.CODE.equals(errorCode)) {
        return new AtHandShakeException(errorText);
      } else if (AtUnauthorizedException.CODE.equals(errorCode)) {
        return new AtUnauthorizedException(errorText);
      } else if (AtInternalServerError.CODE.equals(errorCode)) {
        return new AtInternalServerError(errorText);
      } else if (AtInternalServerException.CODE.equals(errorCode)) {
        return new AtInternalServerException(errorText);
      } else if (AtInboundConnectionLimitException.CODE.equals(errorCode)) {
        return new AtInboundConnectionLimitException(errorText);
      } else if (AtBlockedConnectionException.CODE.equals(errorCode)) {
        return new AtBlockedConnectionException(errorText);
      } else if (AtKeyNotFoundException.CODE.equals(errorCode)) {
        return new AtKeyNotFoundException(errorText);
      } else if (AtInvalidAtKeyException.CODE.equals(errorCode)) {
        return new AtInvalidAtKeyException(errorText);
      } else if (AtSecondaryConnectException.CODE.equals(errorCode)) {
        return new AtSecondaryConnectException(errorText);
      } else if (AtIllegalArgumentException.CODE.equals(errorCode)) {
        return new AtIllegalArgumentException(errorText);
      } else if (AtTimeoutException.CODE.equals(errorCode)) {
        return new AtTimeoutException(errorText);
      } else if (AtServerIsPausedException.CODE.equals(errorCode)) {
        return new AtServerIsPausedException(errorText);
      } else if (AtUnauthenticatedException.CODE.equals(errorCode)) {
        return new AtUnauthenticatedException(errorText);
      }

      return new AtNewErrorCodeWhoDisException(errorCode, errorText);
    }
  }

  /**
   * Value class for hostname and port tuple
   */
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

  /**
   * Represents something that, given an {@link AtSign}, can resolve the {@link Address} of the
   * {@link Secondary} for this {@link AtSign}
   */
  interface AddressFinder {
    Address findSecondary(AtSign atSign) throws IOException, AtSecondaryNotFoundException;
  }
}
