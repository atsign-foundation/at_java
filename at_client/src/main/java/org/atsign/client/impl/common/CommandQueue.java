package org.atsign.client.impl.common;

import static org.atsign.client.impl.common.CommandElement.isConsumerCommand;
import static org.atsign.client.impl.netty.NettyAtCommandExecutor.isPrompt;

import java.util.Collection;
import java.util.Deque;
import java.util.Iterator;
import java.util.Spliterator;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * A queue of {@link CommandElement} elements. This is used to hold pending commands, those
 * commands which have been sent but no response has been received (or event commands
 * where there will be a stream of responses). It is also used to hold queued commands
 * in the event that the pending {@link CommandQueue} is full.
 */
public class CommandQueue implements Iterable<CommandElement> {

  private final int capacity;

  private final Deque<CommandElement> commands = new ConcurrentLinkedDeque<>();

  private final Deque<CommandElement> consumers = new ConcurrentLinkedDeque<>();

  public CommandQueue(int capacity) {
    this.capacity = capacity;
  }

  public void clear() {
    commands.clear();
    consumers.clear();
  }

  public int drain(CommandQueue queue) {
    CommandElement command;
    int count = 0;
    while ((command = queue.poll()) != null) {
      commands.addFirst(command);
      count++;
    }
    return count;
  }

  public void removeIf(Predicate<CommandElement> predicate) {
    commands.removeIf(predicate);
  }

  public boolean offer(CommandElement command) {
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

  public CommandElement pop(String response) {
    CommandElement command = null;
    if (isPrompt(response)) {
      if (isConsumerCommand(commands.peek())) {
        command = commands.pop();
        consumers.add(command);
      }
    } else if (CommandElement.isConsumerResponse(response)) {
      if (isConsumerCommand(commands.peek())) {
        consumers.add(pollFirst(response));
      }
      command = consumers.peek();
    } else {
      command = pollFirst(response);
    }
    return command;
  }

  public CommandElement poll() {
    return commands.pollFirst();
  }

  public CommandElement peek() {
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

  public Collection<CommandElement> pollTimedOut(long timeoutMillis) {
    Collection<CommandElement> list = commands.stream()
        .filter(command -> command.isTimedOut(timeoutMillis))
        .collect(Collectors.toList());
    commands.removeAll(list);
    return list;
  }

  public Collection<CommandElement> pollIsOnReady() {
    Collection<CommandElement> list = commands.stream()
        .filter(command -> command.isOnReady())
        .collect(Collectors.toList());
    commands.removeAll(list);
    return list;
  }

  @Override
  public Iterator<CommandElement> iterator() {
    return commands.iterator();
  }

  @Override
  public void forEach(Consumer<? super CommandElement> action) {
    commands.forEach(action);
  }

  @Override
  public Spliterator<CommandElement> spliterator() {
    return commands.spliterator();
  }

  public boolean hasConsumerCommand() {
    if (!consumers.isEmpty()) {
      return true;
    }
    return commands.stream().filter(c -> c.isConsumerCommand()).findAny().orElse(null) != null;
  }

  private CommandElement pollFirst(String response) {
    Iterator<CommandElement> it = commands.iterator();
    while (it.hasNext()) {
      CommandElement command = it.next();
      if (command.isMatch(response)) {
        it.remove();
        return command;
      }
    }
    return null;
  }
}
