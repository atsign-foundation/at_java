package org.atsign.client.connection.common;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;

import org.junit.jupiter.api.Test;

class CommandTest {

  @Test
  void testToStringReturnsText() {
    Command command = new Command("scan", new CompletableFuture<>(), 100, false);

    assertThat(command.toString(), equalTo("scan"));
  }

  @Test
  void testIsTimedOutTrueWhenTimestampBeforeTimeout() {
    Command command = new Command("cmd", new CompletableFuture<>(), 100, false);

    assertThat(command.isTimedOut(101L), is(true));
  }

  @Test
  void testIsTimedOutFalseWhenTimestampAfterTimeout() {
    Command command = new Command("cmd", new CompletableFuture<>(), 300, false);

    assertThat(command.isTimedOut(200L), is(false));
  }

  @Test
  void testIsTimedOutFalseWhenTimestampEqualsTimeout() {
    Command command = new Command("cmd", new CompletableFuture<>(), 300, false);

    assertThat(command.isTimedOut(300L), is(false));
  }

  @Test
  void testCompleteSetsFutureValue() throws Exception {
    CompletableFuture<String> future = new CompletableFuture<>();
    Command command = new Command("cmd", future, 0, false);

    command.complete("result");

    assertThat(future.get(), is("result"));
  }

  @Test
  void testCompleteAllowsNullValue() throws Exception {
    CompletableFuture<String> future = new CompletableFuture<>();
    Command command = new Command("cmd", future, 0, false);

    command.complete(null);

    assertThat(future.get(), nullValue());
  }

  @Test
  void testCompleteConsumerCommandCompletesFutureAndCallsConsumer() {
    CompletableFuture<Void> future = new CompletableFuture<>();
    Consumer<String> consumer = mock(Consumer.class);

    Command command = new Command("monitor", future, consumer, 0, false);

    command.complete("notification:xyz");

    assertThat(future.isDone(), is(true));
    verify(consumer).accept("notification:xyz");
  }

  @Test
  void testCompleteConsumerCommandDoesNotCallConsumerWhenNullValue() {
    CompletableFuture<Void> future = new CompletableFuture<>();
    Consumer<String> consumer = mock(Consumer.class);

    Command command = new Command("cmd", future, consumer, 0, false);

    command.complete(null);

    assertThat(future.isDone(), is(true));
    verify(consumer, never()).accept("notification:xyz");
  }

  @Test
  void testCompleteExceptionallyCompletesFutureExceptionally() {
    CompletableFuture<String> future = new CompletableFuture<>();
    Command command = new Command("cmd", future, 0, false);

    command.completeExceptionally(new RuntimeException("deliberate"));

    assertThat(future.isCompletedExceptionally(), is(true));
    ExecutionException ex = assertThrows(ExecutionException.class, future::get);
    assertThat(ex.getMessage(), containsString("deliberate"));
  }

  @Test
  void testIsMatchReturnsTrueForCommand() {
    Command command = new Command("scan", new CompletableFuture<>(), 0, false);

    assertThat(command.isMatch("xyz"), is(true));
  }

  @Test
  void testIsMatchReturnsExpectedResultForConsumerCommand() {
    Command command = new Command("monitor", new CompletableFuture<>(), s -> {
    }, 0, false);

    assertThat(command.isMatch("notification:xyz"), is(true));
    assertThat(command.isMatch("ok"), is(false));
  }

  @Test
  void testIsConsumerCommandReturnsExpectedResults() {
    Command command = new Command("scan", new CompletableFuture<>(), 0, false);
    Command consumerCommand = new Command("monitor", new CompletableFuture<>(), s -> {
    }, 0, false);

    assertThat(command.isConsumerCommand(), is(false));
    assertThat(consumerCommand.isConsumerCommand(), is(true));
  }

  @Test
  void testIsConsumerCommandStaticMethodReturnsExpectedResults() {
    Command command = new Command("scan", new CompletableFuture<>(), 0, false);
    Command consumerCommand = new Command("monitor", new CompletableFuture<>(), s -> {
    }, 0, false);

    assertThat(Command.isConsumerCommand(consumerCommand), is(true));
    assertThat(Command.isConsumerCommand(command), is(false));
    assertThat(Command.isConsumerCommand(null), is(false));
  }

  @Test
  void testIsConsumerResponseReturnsExpectedResults() {
    assertThat(Command.isConsumerResponse("notification:test"), is(true));
    assertThat(Command.isConsumerResponse("ok"), is(false));
  }
}
