package org.atsign.client.api.impl.events;

import static org.atsign.client.api.AtEvents.*;

import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 *
 */
public class SimpleAtEventBus implements AtEventBus {
    private final ExecutorService executor = Executors.newCachedThreadPool();

    final Map<AtEventListener, Set<AtEventType>> eventListeners = new HashMap<>();
    @SuppressWarnings("unused")
    public Map<AtEventListener, Set<AtEventType>> getEventListeners() {
        return Collections.unmodifiableMap(eventListeners);
    }

    @Override
    public int publishEvent(AtEventType eventType, Map<String, Object> eventData) {
        Set<Map.Entry<AtEventListener, Set<AtEventType>>> listenerEntries = eventListeners.entrySet();
        int listenerCount = 0;
        for (Map.Entry<AtEventListener, Set<AtEventType>> next : listenerEntries) {
            try {
                Set<AtEventType> eventTypes = next.getValue();
                if (eventTypes.contains(eventType)) {
                    AtEventListener listener = next.getKey();
                    listenerCount++;
                    executor.submit(() -> listener.handleEvent(eventType, eventData));
                }
            } catch (Exception e) {
                System.err.println(this.getClass().getSimpleName() + " caught exception from one of its event listeners : " + e.getMessage());
                e.printStackTrace(System.err);
            }
        }
        return listenerCount;
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
