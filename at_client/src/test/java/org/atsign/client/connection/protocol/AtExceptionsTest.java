package org.atsign.client.connection.protocol;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;

import org.atsign.client.connection.api.AtClientConnection;
import org.atsign.client.connection.protocol.AtExceptions.AtClientConnectionCommand;
import org.atsign.common.AtException;
import org.atsign.common.exceptions.*;
import org.atsign.common.exceptions.AtExceptions.AtInvalidSyntaxException;
import org.atsign.common.exceptions.AtExceptions.AtServerRuntimeException;
import org.junit.jupiter.api.Test;

public class AtExceptionsTest {

  @Test
  public void testServerRuntimeExceptionMapping() {
    AtException e = AtExceptions.toTypedException(AtServerRuntimeException.CODE, "msg");
    assertThat(e, instanceOf(AtServerRuntimeException.class));
    assertThat(e.getMessage(), is("msg"));
  }

  @Test
  public void testInvalidSyntaxExceptionMapping() {
    AtException e = AtExceptions.toTypedException(AtInvalidSyntaxException.CODE, "msg");
    assertThat(e, instanceOf(AtInvalidSyntaxException.class));
  }

  @Test
  public void testBufferOverflowExceptionMapping() {
    AtException e = AtExceptions.toTypedException(AtBufferOverFlowException.CODE, "msg");
    assertThat(e, instanceOf(AtBufferOverFlowException.class));
  }

  @Test
  public void testOutboundConnectionLimitExceptionMapping() {
    AtException e = AtExceptions.toTypedException(AtOutboundConnectionLimitException.CODE, "msg");
    assertThat(e, instanceOf(AtOutboundConnectionLimitException.class));
  }

  @Test
  public void testSecondaryNotFoundExceptionMapping() {
    AtException e = AtExceptions.toTypedException(AtSecondaryNotFoundException.CODE, "msg");
    assertThat(e, instanceOf(AtSecondaryNotFoundException.class));
  }

  @Test
  public void testHandshakeExceptionMapping() {
    AtException e = AtExceptions.toTypedException(AtHandShakeException.CODE, "msg");
    assertThat(e, instanceOf(AtHandShakeException.class));
  }

  @Test
  public void testUnauthorizedExceptionMapping() {
    AtException e = AtExceptions.toTypedException(AtUnauthorizedException.CODE, "msg");
    assertThat(e, instanceOf(AtUnauthorizedException.class));
  }

  @Test
  public void testInternalServerErrorMapping() {
    AtException e = AtExceptions.toTypedException(AtInternalServerError.CODE, "msg");
    assertThat(e, instanceOf(AtInternalServerError.class));
  }

  @Test
  public void testInternalServerExceptionMapping() {
    AtException e = AtExceptions.toTypedException(AtInternalServerException.CODE, "msg");
    assertThat(e, instanceOf(AtInternalServerException.class));
  }

  @Test
  public void testInboundConnectionLimitExceptionMapping() {
    AtException e = AtExceptions.toTypedException(AtInboundConnectionLimitException.CODE, "msg");
    assertThat(e, instanceOf(AtInboundConnectionLimitException.class));
  }

  @Test
  public void testBlockedConnectionExceptionMapping() {
    AtException e = AtExceptions.toTypedException(AtBlockedConnectionException.CODE, "msg");
    assertThat(e, instanceOf(AtBlockedConnectionException.class));
  }

  @Test
  public void testKeyNotFoundExceptionMapping() {
    AtException e = AtExceptions.toTypedException(AtKeyNotFoundException.CODE, "msg");
    assertThat(e, instanceOf(AtKeyNotFoundException.class));
  }

  @Test
  public void testInvalidAtKeyExceptionMapping() {
    AtException e = AtExceptions.toTypedException(AtInvalidAtKeyException.CODE, "msg");
    assertThat(e, instanceOf(AtInvalidAtKeyException.class));
  }

  @Test
  public void testSecondaryConnectExceptionMapping() {
    AtException e = AtExceptions.toTypedException(AtSecondaryConnectException.CODE, "msg");
    assertThat(e, instanceOf(AtSecondaryConnectException.class));
  }

  @Test
  public void testIllegalArgumentExceptionMapping() {
    AtException e = AtExceptions.toTypedException(AtIllegalArgumentException.CODE, "msg");
    assertThat(e, instanceOf(AtIllegalArgumentException.class));
  }

  @Test
  public void testTimeoutExceptionMapping() {
    AtException e = AtExceptions.toTypedException(AtTimeoutException.CODE, "msg");
    assertThat(e, instanceOf(AtTimeoutException.class));
  }

  @Test
  public void testServerIsPausedExceptionMapping() {
    AtException e = AtExceptions.toTypedException(AtServerIsPausedException.CODE, "msg");
    assertThat(e, instanceOf(AtServerIsPausedException.class));
  }

  @Test
  public void testUnauthenticatedExceptionMapping() {
    AtException e = AtExceptions.toTypedException(AtUnauthenticatedException.CODE, "msg");
    assertThat(e, instanceOf(AtUnauthenticatedException.class));
  }

  @Test
  public void testUnknownCodeReturnsNewErrorCodeException() {
    AtException e = AtExceptions.toTypedException("UNKNOWN_CODE", "msg");
    assertThat(e, instanceOf(AtNewErrorCodeWhoDisException.class));
  }

  @Test
  public void testThrowOnReadyExceptionWrapsAtException() {
    AtClientConnectionCommand command = connection -> {
      throw new AtUnauthorizedException("deliberate");
    };
    Consumer<AtClientConnection> consumer = AtExceptions.throwOnReadyException(command);
    AtClientConnection connection = mock(AtClientConnection.class);

    AtOnReadyException ex = assertThrows(AtOnReadyException.class, () -> consumer.accept(connection));
    assertThat(ex.getMessage(), containsString("deliberate"));
    assertThat(ex.getCause(), instanceOf(AtUnauthorizedException.class));
  }

  @Test
  public void testThrowOnReadyExceptionWrapsExecutionException() {
    AtClientConnectionCommand command = connection -> {
      throw new ExecutionException(new RuntimeException("deliberate"));
    };
    Consumer<AtClientConnection> consumer = AtExceptions.throwOnReadyException(command);
    AtClientConnection connection = mock(AtClientConnection.class);

    RuntimeException ex = assertThrows(RuntimeException.class, () -> consumer.accept(connection));
    assertThat(ex.getCause(), instanceOf(ExecutionException.class));
  }

  @Test
  public void testThrowOnReadyExceptionWrapsInterruptedException() {
    AtClientConnectionCommand command = connection -> {
      throw new InterruptedException("deliberate");
    };
    Consumer<AtClientConnection> consumer = AtExceptions.throwOnReadyException(command);
    AtClientConnection connection = mock(AtClientConnection.class);

    RuntimeException ex = assertThrows(RuntimeException.class, () -> consumer.accept(connection));
    assertThat(ex.getCause(), instanceOf(InterruptedException.class));
  }

  @Test
  public void testThrowOnReadyExceptionInvokesWrappedCommand()
      throws AtException, ExecutionException, InterruptedException {
    AtClientConnectionCommand command = mock(AtClientConnectionCommand.class);
    Consumer<AtClientConnection> consumer = AtExceptions.throwOnReadyException(command);
    AtClientConnection connection = mock(AtClientConnection.class);

    consumer.accept(connection);

    verify(command).run(connection);
  }
}
