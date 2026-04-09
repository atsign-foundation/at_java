package org.atsign.client.impl.common;

import io.netty.util.concurrent.DefaultThreadFactory;
import lombok.extern.slf4j.Slf4j;

import static org.atsign.client.api.AtEvents.AtEventBus;
import static org.atsign.client.api.AtEvents.AtEventListener;
import static org.atsign.client.api.AtEvents.AtEventType;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

/**
 * Simple implementation of {@link AtEventBus} which will asynchronously dispatch
 * events to registered listeners.
 */
@Slf4j
public class SimpleAtEventBus implements AtEventBus {

  private final ExecutorService executor;

  final Map<AtEventListener, Set<AtEventType>> eventListeners = new ConcurrentHashMap<>();

  public SimpleAtEventBus(ExecutorService executor) {
    this.executor = executor;
  }

  public SimpleAtEventBus() {
    this(Executors.newSingleThreadExecutor(new DefaultThreadFactory("eventbus", true)));
  }

  @SuppressWarnings("unused")
  public Map<AtEventListener, Set<AtEventType>> getEventListeners() {
    return Collections.unmodifiableMap(eventListeners);
  }

  @Override
  public int publishEvent(AtEventType eventType, Map<String, Object> eventData) {
    List<AtEventListener> listeners = eventListeners.entrySet().stream()
        .filter(entry -> entry.getValue().contains(eventType))
        .map(Map.Entry::getKey)
        .collect(Collectors.toList());
    executor.submit(() -> invokeListeners(eventType, eventData, listeners));
    return listeners.size();
  }

  private static void invokeListeners(AtEventType eventType, Map<String, Object> eventData,
                                      List<AtEventListener> listeners) {
    for (AtEventListener listener : listeners) {
      try {
        listener.handleEvent(eventType, eventData);
      } catch (Exception e) {
        log.error("event listener exception", e);
      }
    }
  }

  @Override
  public void addEventListener(AtEventListener listener, Set<AtEventType> eventTypes) {
    eventListeners.put(listener, eventTypes);
    // TODO if the eventTypes include a monitor-originating event type
    // TODO then publish a monitorStartRequest event after adding the listener
  }

  @Override
  public void removeEventListener(AtEventListener listener) {
    // TODO If there will be no event listeners remaining for any monitor-originating event types
    // TODO then publish a monitorStopRequest event before removing the listener
    eventListeners.remove(listener);
  }
}
