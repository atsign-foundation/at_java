package org.atsign.client.connection.protocol;

import org.atsign.client.connection.api.AtClientConnection;
import org.atsign.common.AtException;
import org.atsign.common.exceptions.*;
import org.atsign.common.exceptions.AtExceptions.AtInvalidSyntaxException;
import org.atsign.common.exceptions.AtExceptions.AtServerRuntimeException;

import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;

/**
 * Utilities for throwing and handling typed {@link AtException} instances
 */
public class AtExceptions {

  public static AtException toTypedException(String code, String text) {
    if (AtServerRuntimeException.CODE.equals(code)) {
      return new AtServerRuntimeException(text);
    } else if (AtInvalidSyntaxException.CODE.equals(code)) {
      return new AtInvalidSyntaxException(text);
    } else if (AtBufferOverFlowException.CODE.equals(code)) {
      return new AtBufferOverFlowException(text);
    } else if (AtOutboundConnectionLimitException.CODE.equals(code)) {
      return new AtOutboundConnectionLimitException(text);
    } else if (AtSecondaryNotFoundException.CODE.equals(code)) {
      return new AtSecondaryNotFoundException(text);
    } else if (AtHandShakeException.CODE.equals(code)) {
      return new AtHandShakeException(text);
    } else if (AtUnauthorizedException.CODE.equals(code)) {
      return new AtUnauthorizedException(text);
    } else if (AtInternalServerError.CODE.equals(code)) {
      return new AtInternalServerError(text);
    } else if (AtInternalServerException.CODE.equals(code)) {
      return new AtInternalServerException(text);
    } else if (AtInboundConnectionLimitException.CODE.equals(code)) {
      return new AtInboundConnectionLimitException(text);
    } else if (AtBlockedConnectionException.CODE.equals(code)) {
      return new AtBlockedConnectionException(text);
    } else if (AtKeyNotFoundException.CODE.equals(code)) {
      return new AtKeyNotFoundException(text);
    } else if (AtInvalidAtKeyException.CODE.equals(code)) {
      return new AtInvalidAtKeyException(text);
    } else if (AtSecondaryConnectException.CODE.equals(code)) {
      return new AtSecondaryConnectException(text);
    } else if (AtIllegalArgumentException.CODE.equals(code)) {
      return new AtIllegalArgumentException(text);
    } else if (AtTimeoutException.CODE.equals(code)) {
      return new AtTimeoutException(text);
    } else if (AtServerIsPausedException.CODE.equals(code)) {
      return new AtServerIsPausedException(text);
    } else if (AtUnauthenticatedException.CODE.equals(code)) {
      return new AtUnauthenticatedException(text);
    }

    return new AtNewErrorCodeWhoDisException(code, text);
  }

  /**
   * A Command / Runnable that may throw exceptions.
   */
  public interface AtClientConnectionCommand {
    void run(AtClientConnection connection) throws AtException, ExecutionException, InterruptedException;
  }

  /**
   * Wraps an {@link AtClientConnectionCommand}, typically an
   * {@link AtClientConnection#onReady(Consumer)}
   * command, such that AtExceptions are caught and rethrown as {@link AtOnReadyException}.
   * NOTE: {@link AtOnReadyException}s are regarded as fatal, i.e. the {@link AtClientConnection} is
   * unusable.
   *
   * @param command A command/runnable thatmay throw exceptions.
   * @return a consumer that is designed to be passed as an argument to
   *         {@link AtClientConnection#onReady(Consumer)}
   */
  public static Consumer<AtClientConnection> throwOnReadyException(AtClientConnectionCommand command) {
    return connection -> {
      try {
        command.run(connection);
      } catch (AtException e) {
        // AtOnReadyExceptions are fatal for the connection
        throw new AtOnReadyException(e.getMessage(), e);
      } catch (ExecutionException | InterruptedException e) {
        throw new RuntimeException(e);
      }
    };
  }

}
