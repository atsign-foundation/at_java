package org.atsign.client.impl.common;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;

import org.junit.jupiter.api.Test;

class CommandElementTest {

  @Test
  void testToStringReturnsText() {
    CommandElement command = new CommandElement("scan", new CompletableFuture<>(), 100, false);

    assertThat(command.toString(), equalTo("scan"));
  }

  @Test
  void testIsTimedOutTrueWhenTimestampBeforeTimeout() {
    CommandElement command = new CommandElement("cmd", new CompletableFuture<>(), 100, false);

    assertThat(command.isTimedOut(101L), is(true));
  }

  @Test
  void testIsTimedOutFalseWhenTimestampAfterTimeout() {
    CommandElement command = new CommandElement("cmd", new CompletableFuture<>(), 300, false);

    assertThat(command.isTimedOut(200L), is(false));
  }

  @Test
  void testIsTimedOutFalseWhenTimestampEqualsTimeout() {
    CommandElement command = new CommandElement("cmd", new CompletableFuture<>(), 300, false);

    assertThat(command.isTimedOut(300L), is(false));
  }

  @Test
  void testCompleteSetsFutureValue() throws Exception {
    CompletableFuture<String> future = new CompletableFuture<>();
    CommandElement command = new CommandElement("cmd", future, 0, false);

    command.complete("result");

    assertThat(future.get(), is("result"));
  }

  @Test
  void testCompleteAllowsNullValue() throws Exception {
    CompletableFuture<String> future = new CompletableFuture<>();
    CommandElement command = new CommandElement("cmd", future, 0, false);

    command.complete(null);

    assertThat(future.get(), nullValue());
  }

  @Test
  void testCompleteConsumerCommandCompletesFutureAndCallsConsumer() {
    CompletableFuture<Void> future = new CompletableFuture<>();
    Consumer<String> consumer = mock(Consumer.class);

    CommandElement command = new CommandElement("monitor", future, consumer, 0, false);

    command.complete("notification:xyz");

    assertThat(future.isDone(), is(true));
    verify(consumer).accept("notification:xyz");
  }

  @Test
  void testCompleteConsumerCommandDoesNotCallConsumerWhenNullValue() {
    CompletableFuture<Void> future = new CompletableFuture<>();
    Consumer<String> consumer = mock(Consumer.class);

    CommandElement command = new CommandElement("cmd", future, consumer, 0, false);

    command.complete(null);

    assertThat(future.isDone(), is(true));
    verify(consumer, never()).accept("notification:xyz");
  }

  @Test
  void testCompleteExceptionallyCompletesFutureExceptionally() {
    CompletableFuture<String> future = new CompletableFuture<>();
    CommandElement command = new CommandElement("cmd", future, 0, false);

    command.completeExceptionally(new RuntimeException("deliberate"));

    assertThat(future.isCompletedExceptionally(), is(true));
    ExecutionException ex = assertThrows(ExecutionException.class, future::get);
    assertThat(ex.getMessage(), containsString("deliberate"));
  }

  @Test
  void testIsMatchReturnsTrueForCommand() {
    CommandElement command = new CommandElement("scan", new CompletableFuture<>(), 0, false);

    assertThat(command.isMatch("xyz"), is(true));
  }

  @Test
  void testIsMatchReturnsExpectedResultForConsumerCommand() {
    CommandElement command = new CommandElement("monitor", new CompletableFuture<>(), s -> {
    }, 0, false);

    assertThat(command.isMatch("notification:xyz"), is(true));
    assertThat(command.isMatch("ok"), is(false));
  }

  @Test
  void testIsConsumerCommandReturnsExpectedResults() {
    CommandElement command = new CommandElement("scan", new CompletableFuture<>(), 0, false);
    CommandElement consumerCommand = new CommandElement("monitor", new CompletableFuture<>(), s -> {
    }, 0, false);

    assertThat(command.isConsumerCommand(), is(false));
    assertThat(consumerCommand.isConsumerCommand(), is(true));
  }

  @Test
  void testIsConsumerCommandStaticMethodReturnsExpectedResults() {
    CommandElement command = new CommandElement("scan", new CompletableFuture<>(), 0, false);
    CommandElement consumerCommand = new CommandElement("monitor", new CompletableFuture<>(), s -> {
    }, 0, false);

    assertThat(CommandElement.isConsumerCommand(consumerCommand), is(true));
    assertThat(CommandElement.isConsumerCommand(command), is(false));
    assertThat(CommandElement.isConsumerCommand(null), is(false));
  }

  @Test
  void testIsConsumerResponseReturnsExpectedResults() {
    assertThat(CommandElement.isConsumerResponse("notification:test"), is(true));
    assertThat(CommandElement.isConsumerResponse("ok"), is(false));
  }
}
