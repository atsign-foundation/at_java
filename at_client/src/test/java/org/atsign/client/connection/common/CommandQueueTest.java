package org.atsign.client.connection.common;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class CommandQueueTest {

  private static final int DEFAULT_CAPACITY = 10;

  private CommandQueue commandQueue;

  @BeforeEach
  void setUp() {
    commandQueue = new CommandQueue(DEFAULT_CAPACITY);
  }

  @Test
  void testNewQueueIsEmpty() {
    assertThat(commandQueue.isEmpty(), is(true));
  }

  @Test
  void testGetQueueCapacityReturnsConstructorArg() {
    assertThat(new CommandQueue(2).getQueueCapacity(), equalTo(2));
  }

  @Test
  void testNewQueueHasSizeZero() {
    assertThat(commandQueue.size(), is(0));
  }

  @Test
  void testOfferNonConsumerCommandWithinCapacityReturnsTrue() {
    Command command = new Command("request", new CompletableFuture<>(), 0L, false);

    boolean result = commandQueue.offer(command);

    assertThat(result, is(true));
  }

  @Test
  void testOfferNonConsumerCommandIncreasesSize() {
    Command command = new Command("request", new CompletableFuture<>(), 0L, false);

    commandQueue.offer(command);

    assertThat(commandQueue.size(), is(1));
  }

  @Test
  void testOfferMultipleCommandsIncreasesSize() {
    for (int i = 0; i < 5; i++) {
      Command command = new Command("request", new CompletableFuture<>(), 0L, false);
      commandQueue.offer(command);
    }

    assertThat(commandQueue.size(), is(5));
  }

  @Test
  void testOfferExceedingCapacityReturnsFalse() {
    CommandQueue queue = new CommandQueue(2);

    Command command1 = new Command("request1", new CompletableFuture<>(), 0L, false);
    Command command2 = new Command("request2", new CompletableFuture<>(), 1L, false);
    Command command3 = new Command("request3", new CompletableFuture<>(), 2L, false);

    queue.offer(command1);
    queue.offer(command2);
    boolean result = queue.offer(command3);

    assertThat(result, is(false));
  }

  @Test
  void testOfferExceedingCapacityDoesNotIncreaseSize() {
    CommandQueue smallQueue = new CommandQueue(1);

    Command command1 = new Command("request1", new CompletableFuture<>(), 0L, false);
    Command command2 = new Command("request2", new CompletableFuture<>(), 1L, false);

    smallQueue.offer(command1);
    smallQueue.offer(command2);

    assertThat(smallQueue.size(), is(1));
  }

  @Test
  void testOfferAtExactCapacityReturnsTrue() {
    CommandQueue queue = new CommandQueue(1);

    Command command = new Command("request", new CompletableFuture<>(), 0L, false);

    assertThat(queue.offer(command), is(true));
  }

  @Test
  void testOfferConsumerCommandWhenNoExistingConsumerReturnsTrue() {
    Command command = new Command("request", new CompletableFuture<>(), 0L, false);

    boolean result = commandQueue.offer(command);

    assertThat(result, is(true));
  }

  @Test
  void testOfferSecondConsumerCommandReturnsFalse() {
    Command command1 = new Command("request1", new CompletableFuture<>(), s -> {
    }, 0L, false);
    commandQueue.offer(command1);

    Command command2 = new Command("request1", new CompletableFuture<>(), s -> {
    }, 0L, false);

    commandQueue.offer(command2);
    assertThat(commandQueue.hasConsumerCommand(), is(true));
    assertThat(commandQueue.offer(command2), is(false));

    commandQueue.pop("@");
    assertThat(commandQueue.size(), equalTo(0));
    assertThat(commandQueue.hasConsumerCommand(), is(true));
    assertThat(commandQueue.offer(command2), is(false));
  }

  @Test
  void testPopEmptyQueueReturnsNull() {
    assertThat(commandQueue.pop("response"), nullValue());

    Command command = new Command("request1", new CompletableFuture<>(), s -> {
    }, 0L, false);
    commandQueue.offer(command);
    commandQueue.pop("response1");

    assertThat(commandQueue.pop("response"), nullValue());
  }

  @Test
  void testPopReturnsNullForEventIfNoConsumerCommand() {
    assertThat(commandQueue.pop("notification:xyz"), nullValue());
  }

  @Test
  void testPopReturnsExpectedCommandForResponse() {
    Command command = new Command("request1", new CompletableFuture<>(), 0L, false);
    commandQueue.offer(command);
    commandQueue.pop("@");

    assertThat(commandQueue.pop("response"), equalTo(command));
  }

  @Test
  void testPopReturnsNullForPromptIfNoConsumerCommand() {
    Command command = new Command("request1", new CompletableFuture<>(), 0L, false);
    commandQueue.offer(command);
    assertThat(commandQueue.pop("@"), nullValue());
  }

  @Test
  void testPopReturnsConsumerCommandForPrompt() {
    Command command = new Command("request1", new CompletableFuture<>(), s -> {
    }, 0L, false);
    commandQueue.offer(command);
    assertThat(commandQueue.pop("@"), equalTo(command));
  }

  @Test
  void testPopWithEventReturnsConsumerCommand() {
    Command command = new Command("monitor", new CompletableFuture<>(), s -> {
    }, 0L, false);
    commandQueue.offer(command);
    assertThat(commandQueue.size(), equalTo(1));
    assertThat(commandQueue.isEmpty(), is(false));

    assertThat(commandQueue.pop("@"), equalTo(command));

    assertThat(commandQueue.pop("notification:xyz"), equalTo(command));
    assertThat(commandQueue.size(), equalTo(0));
    assertThat(commandQueue.isEmpty(), is(true));
  }

  @Test
  void testPopWithEventReturnsPeekedConsumerCommand() {
    Command command = new Command("monitor", new CompletableFuture<>(), s -> {
    }, 0L, false);
    commandQueue.offer(command);
    assertThat(commandQueue.size(), equalTo(1));
    assertThat(commandQueue.isEmpty(), is(false));

    assertThat(commandQueue.pop("notification:xyz"), equalTo(command));
    assertThat(commandQueue.size(), equalTo(0));
    assertThat(commandQueue.isEmpty(), is(true));
  }

  @Test
  void testPollReturnsFirstIn() {
    Command command1 = new Command("request1", new CompletableFuture<>(), 0L, false);
    Command command2 = new Command("request2", new CompletableFuture<>(), 0L, false);

    commandQueue.offer(command1);
    commandQueue.offer(command2);
    assertThat(commandQueue.size(), equalTo(2));

    assertThat(commandQueue.poll(), equalTo(command1));
    assertThat(commandQueue.size(), equalTo(1));
    assertThat(commandQueue.poll(), equalTo(command2));
    assertThat(commandQueue.size(), equalTo(0));
    assertThat(commandQueue.poll(), nullValue());
  }

  @Test
  void testPeekReturnsFirstIn() {
    Command command1 = new Command("request1", new CompletableFuture<>(), 0L, false);
    Command command2 = new Command("request2", new CompletableFuture<>(), 0L, false);

    commandQueue.offer(command1);
    commandQueue.offer(command2);
    assertThat(commandQueue.size(), equalTo(2));

    assertThat(commandQueue.peek(), equalTo(command1));
    assertThat(commandQueue.size(), equalTo(2));
    assertThat(commandQueue.peek(), equalTo(command1));
    commandQueue.poll();
    assertThat(commandQueue.peek(), equalTo(command2));
    commandQueue.poll();
    assertThat(commandQueue.peek(), nullValue());
  }

  @Test
  void testIsEmptyReturnsTrueWhenQueueHasNoCommands() {
    assertThat(commandQueue.isEmpty(), is(true));
  }

  @Test
  void testIsEmptyReturnsFalseAfterCommandOffered() {
    Command command = new Command("request", new CompletableFuture<>(), 0L, false);
    commandQueue.offer(command);

    assertThat(commandQueue.isEmpty(), is(false));
  }

  @Test
  void testSizeReflectsNumberOfCommandsInQueue() {
    int count = 4;
    for (int i = 0; i < count; i++) {
      Command command = new Command("request", new CompletableFuture<>(), i, false);
      commandQueue.offer(command);
      assertThat(commandQueue.size(), is(i + 1));
    }
  }

  @Test
  void testIteratorReturnsAllOfferedCommands() {
    Command command1 = new Command("request1", new CompletableFuture<>(), 0L, false);
    Command command2 = new Command("request2", new CompletableFuture<>(), 1L, false);

    commandQueue.offer(command1);
    commandQueue.offer(command2);

    List<Command> iterated = new ArrayList<>();
    commandQueue.iterator().forEachRemaining(iterated::add);

    assertThat(iterated, hasSize(2));
    assertThat(iterated, containsInAnyOrder(command1, command2));
  }

  @Test
  void testIteratorOnEmptyQueueHasNoElements() {
    assertThat(commandQueue.iterator().hasNext(), is(false));
  }

  @Test
  void testForEachVisitsAllCommands() {
    Command command1 = new Command("request1", new CompletableFuture<>(), 0L, false);
    Command command2 = new Command("request2", new CompletableFuture<>(), 1L, false);

    commandQueue.offer(command1);
    commandQueue.offer(command2);

    List<Command> visited = new ArrayList<>();
    commandQueue.forEach(visited::add);

    assertThat(visited, hasSize(2));
    assertThat(visited, containsInAnyOrder(command1, command2));
  }

  @Test
  void testSpliteratorCoversAllCommands() {
    Command command1 = new Command("monitor", new CompletableFuture<>(), s -> {
    }, 0L, false);
    Command command2 = new Command("request1", new CompletableFuture<>(), 0L, false);
    Command command3 = new Command("request2", new CompletableFuture<>(), 1L, false);

    commandQueue.offer(command1);
    commandQueue.offer(command2);
    commandQueue.offer(command3);

    List<Command> collected = new ArrayList<>();
    commandQueue.spliterator().forEachRemaining(collected::add);

    assertThat(collected, hasSize(3));
    assertThat(collected, containsInAnyOrder(command1, command2, command3));

    commandQueue.pop("notification:xyz");
    collected = new ArrayList<>();
    commandQueue.spliterator().forEachRemaining(collected::add);

    assertThat(collected, hasSize(2));
    assertThat(collected, containsInAnyOrder(command2, command3));

    commandQueue.pop("response1");
    collected = new ArrayList<>();
    commandQueue.spliterator().forEachRemaining(collected::add);

    assertThat(collected, hasSize(1));
    assertThat(collected, containsInAnyOrder(command3));
  }

  @Test
  void testOfferToZeroCapacityQueueReturnsFalse() {
    CommandQueue zeroQueue = new CommandQueue(0);
    Command command = new Command("request", new CompletableFuture<>(), 0L, false);

    boolean result = zeroQueue.offer(command);

    assertThat(result, is(false));
  }

  @Test
  void testZeroCapacityQueueRemainsEmpty() {
    CommandQueue zeroQueue = new CommandQueue(0);
    Command command = new Command("request", new CompletableFuture<>(), 0L, false);
    zeroQueue.offer(command);

    assertThat(zeroQueue.isEmpty(), is(true));
  }

  @Test
  void testPollTimedOut() {
    Command command1 = new Command("request1", new CompletableFuture<>(), 1L, false);
    Command command2 = new Command("request2", new CompletableFuture<>(), 1L, false);
    Command command3 = new Command("request3", new CompletableFuture<>(), 2L, false);

    commandQueue.offer(command1);
    commandQueue.offer(command2);
    commandQueue.offer(command3);

    assertThat(commandQueue.pollTimedOut(0L), is(empty()));
    assertThat(commandQueue.size(), equalTo(3));
    assertThat(commandQueue.pollTimedOut(1L), is(empty()));
    assertThat(commandQueue.size(), equalTo(3));
    assertThat(commandQueue.pollTimedOut(2L), containsInAnyOrder(command1, command2));
    assertThat(commandQueue.size(), equalTo(1));
    assertThat(commandQueue.pollTimedOut(3L), containsInAnyOrder(command3));
    assertThat(commandQueue.size(), equalTo(0));
  }


}
