package org.atsign.client.impl.common;

import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

/**
 * An "element" / holder for At Protocol commands.
 * This is intended to be used by {@link org.atsign.client.api.AtCommandExecutor} implementations.
 * This holder maintains the association to a {@link CompletableFuture} and the {@link Consumer}
 * (if the command generates a stream of events). It also holds context information such as a
 * timestamp and whether the command was sent from an onReady {@link Consumer}.
 */
public class CommandElement {

  private final String text;

  private final CompletableFuture<?> future;

  private final Consumer<String> consumer;

  private final long timestampMillis;

  private final boolean isOnReady;

  public CommandElement(String text, CompletableFuture<String> future, long timestampMillis, boolean isOnReady) {
    this.text = text;
    this.future = future;
    this.consumer = null;
    this.timestampMillis = timestampMillis;
    this.isOnReady = isOnReady;
  }

  public CommandElement(String text, CompletableFuture<Void> future, Consumer<String> consumer, long timestampMillis,
                        boolean isOnReady) {
    this.text = text;
    this.future = future;
    this.consumer = consumer;
    this.timestampMillis = timestampMillis;
    this.isOnReady = isOnReady;
  }

  @Override
  public String toString() {
    return text;
  }

  public boolean isTimedOut(long timeoutMillis) {
    return timestampMillis < timeoutMillis;
  }

  public boolean isOnReady() {
    return isOnReady;
  }

  public void completeExceptionally(Throwable ex) {
    future.completeExceptionally(ex);
  }

  public void complete(String s) {
    if (consumer != null) {
      future.complete(null);
      if (s != null) {
        consumer.accept(s);
      }
    } else {
      //noinspection unchecked
      ((CompletableFuture<String>) future).complete(s);
    }
  }

  public boolean isMatch(String response) {
    if (consumer != null) {
      if (isErrorResponse(response)) {
        return true;
      } else {
        return text.startsWith("monitor") && response.startsWith("notification:");
      }
    } else {
      // currently no way to match responses to commands
      return true;
    }
  }

  public boolean isConsumerCommand() {
    return consumer != null;
  }

  public static boolean isConsumerCommand(CommandElement command) {
    return command != null && command.isConsumerCommand();
  }

  public static boolean isConsumerResponse(String s) {
    return s.startsWith("notification:");
  }

  public static boolean isErrorResponse(String s) {
    return s.startsWith("error:");
  }

  public static boolean isPrompt(String s) {
    return s.startsWith("@") && s.endsWith("@");
  }

}
