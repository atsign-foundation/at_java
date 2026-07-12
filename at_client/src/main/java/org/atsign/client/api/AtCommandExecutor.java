package org.atsign.client.api;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;

/**
 * Represents something that is capable of sending At Protocol commands,
 * and receiving responses, to and from At Server command interface.
 * {@link AtCommandExecutor}s are closeable resources and should be treated accordingly.
 */
public interface AtCommandExecutor extends AutoCloseable {

  /**
   * Asynchronously executes an At Protocol command that is expected to return a response.
   *
   * @param command the command to execute
   * @param future the future which will be completed with the response
   */
  void send(String command, CompletableFuture<String> future);

  /**
   * Asynchronously executes an At Protocol command that is expected to return a response.
   *
   * @param command the command to execute
   * @return the future which will be completed with the response
   */
  default CompletableFuture<String> send(String command) {
    CompletableFuture<String> future = new CompletableFuture<>();
    send(command, future);
    return future;
  }

  /**
   * Synchronously executes an At Protocol command that is expected to return a response.
   * This method will return once the command has been sent to the AtSign server and the response
   * has been received.
   *
   * @param command the command to execute
   * @return the response
   */
  String sendSync(String command) throws ExecutionException, InterruptedException;

  /**
   * Asynchronously executes an At Protocol command that is expected to return a stream of events.
   *
   * @param command the command to execute
   * @param consumer the consumer which the connection will invoke with each event
   * @param future the future which will be completed when the command is sent and acknowledged
   */
  void send(String command, Consumer<String> consumer, CompletableFuture<Void> future);

  /**
   * Asynchronously executes an At Protocol command that is expected to return a stream of events.
   *
   * @param command the command to execute
   * @param consumer the consumer which the connection will invoke with each event
   * @return a future which will be completed when the command is sent and acknowledged
   */
  default CompletableFuture<Void> send(String command, Consumer<String> consumer) {
    CompletableFuture<Void> future = new CompletableFuture<>();
    send(command, consumer, future);
    return future;
  }

  /**
   * Synchronously executes an At Protocol command that is expected to return a stream of events.
   * This method will return once the command has been sent to the AtSign server and some form of
   * acknowledgement has been received.
   *
   * @param command the command to execute
   * @param consumer the consumer which the connection will invoke with each event
   */
  void sendSync(String command, Consumer<String> consumer) throws ExecutionException, InterruptedException;

  /**
   * Registers a consumer that will be invoked when the connection is ready for sending commands.
   * This is intended to be used to execute commands that set up state, for example to ensure that
   * a connection is authenticated or that a connection is registered for notifications.
   * If this connection is already ready then the consumer will be invoked immediately.
   *
   * @param consumer a consumer that will perform commands
   * @return this (to allow chaining / fluent style invocation)
   */
  AtCommandExecutor onReady(Consumer<AtCommandExecutor> consumer);

  /**
   * Returns this executor's {@link AtCommandExecutorContext authentication context}: the identity it
   * authenticates as together with the single-use challenge from the {@code from:} it issued as its
   * first command once ready. The authentication commands reuse that challenge rather than sending a
   * second {@code from:}, and read the identity from the same context. Never {@code null} — an
   * executor that was not configured with an atSign (and so issued no initial {@code from:}) returns
   * {@link AtCommandExecutorContext#EMPTY}, whose challenge is always {@code null}.
   *
   * @return this executor's authentication context (never {@code null})
   */
  default AtCommandExecutorContext getContext() {
    return AtCommandExecutorContext.EMPTY;
  }
}
