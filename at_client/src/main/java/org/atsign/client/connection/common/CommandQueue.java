package org.atsign.client.connection.common;

import static org.atsign.client.connection.common.Command.isConsumerCommand;
import static org.atsign.client.connection.netty.NettyAtClientConnection.isPrompt;

import java.util.Collection;
import java.util.Deque;
import java.util.Iterator;
import java.util.Spliterator;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * A queue of {@link Command} elements. This is used to hold pending commands, those
 * commands which have been sent but no response has been received (or event commands
 * where there will be a stream of responses). It is also used to hold queued commands
 * in the event that the pending {@link CommandQueue} is full.
 */
public class CommandQueue implements Iterable<Command> {

  private final int capacity;

  private final Deque<Command> commands = new ConcurrentLinkedDeque<>();

  private final Deque<Command> consumers = new ConcurrentLinkedDeque<>();

  public CommandQueue(int capacity) {
    this.capacity = capacity;
  }

  public void clear() {
    commands.clear();
    consumers.clear();
  }

  public int drain(CommandQueue queue) {
    Command command;
    int count = 0;
    while ((command = queue.poll()) != null) {
      commands.addFirst(command);
      count++;
    }
    return count;
  }

  public void removeIf(Predicate<Command> predicate) {
    commands.removeIf(predicate);
  }

  public boolean offer(Command command) {
    if (command.isConsumerCommand() && hasConsumerCommand()) {
      // TODO: allow replacement of existing command where the verb matches
      return false;
    }
    if (commands.size() + 1 <= capacity) {
      commands.addLast(command);
      return true;
    } else {
      return false;
    }
  }

  public Command pop(String response) {
    Command command = null;
    if (isPrompt(response)) {
      if (isConsumerCommand(commands.peek())) {
        command = commands.pop();
        consumers.add(command);
      }
    } else if (Command.isConsumerResponse(response)) {
      if (isConsumerCommand(commands.peek())) {
        consumers.add(pollFirst(response));
      }
      command = consumers.peek();
    } else {
      command = pollFirst(response);
    }
    return command;
  }

  public Command poll() {
    return commands.pollFirst();
  }

  public Command peek() {
    return commands.peekFirst();
  }

  public int size() {
    return commands.size();
  }

  public boolean isEmpty() {
    return commands.isEmpty();
  }

  public int getQueueCapacity() {
    return capacity;
  }

  public Collection<Command> pollTimedOut(long timeoutMillis) {
    Collection<Command> list = commands.stream()
        .filter(command -> command.isTimedOut(timeoutMillis))
        .collect(Collectors.toList());
    commands.removeAll(list);
    return list;
  }

  public Collection<Command> pollIsOnReady() {
    Collection<Command> list = commands.stream()
        .filter(command -> command.isOnReady())
        .collect(Collectors.toList());
    commands.removeAll(list);
    return list;
  }

  @Override
  public Iterator<Command> iterator() {
    return commands.iterator();
  }

  @Override
  public void forEach(Consumer<? super Command> action) {
    commands.forEach(action);
  }

  @Override
  public Spliterator<Command> spliterator() {
    return commands.spliterator();
  }

  public boolean hasConsumerCommand() {
    if (!consumers.isEmpty()) {
      return true;
    }
    return commands.stream().filter(c -> c.isConsumerCommand()).findAny().orElse(null) != null;
  }

  private Command pollFirst(String response) {
    Iterator<Command> it = commands.iterator();
    while (it.hasNext()) {
      Command command = it.next();
      if (command.isMatch(response)) {
        it.remove();
        return command;
      }
    }
    return null;
  }
}
