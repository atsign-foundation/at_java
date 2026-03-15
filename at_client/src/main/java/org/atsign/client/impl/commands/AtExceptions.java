package org.atsign.client.impl.commands;

import org.atsign.client.api.AtCommandExecutor;
import org.atsign.client.impl.exceptions.*;

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
    void run(AtCommandExecutor executor) throws AtException, ExecutionException, InterruptedException;
  }

  /**
   * Wraps an {@link AtClientConnectionCommand}, typically an
   * {@link AtCommandExecutor#onReady(Consumer)}
   * command, such that AtExceptions are caught and rethrown as {@link AtOnReadyException}.
   * NOTE: {@link AtOnReadyException}s are regarded as fatal, i.e. the {@link AtCommandExecutor} is
   * unusable.
   *
   * @param command A command/runnable thatmay throw exceptions.
   * @return a consumer that is designed to be passed as an argument to
   *         {@link AtCommandExecutor#onReady(Consumer)}
   */
  public static Consumer<AtCommandExecutor> throwOnReadyException(AtClientConnectionCommand command) {
    return executor -> {
      try {
        command.run(executor);
      } catch (AtException e) {
        // AtOnReadyExceptions are fatal for the executor
        throw new AtOnReadyException(e.getMessage(), e);
      } catch (ExecutionException | InterruptedException e) {
        throw new RuntimeException(e);
      }
    };
  }

}
