package org.atsign.client.impl.common;

import org.atsign.client.api.AtEvents;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;

import static org.atsign.client.api.AtEvents.AtEventType.decryptedUpdateNotification;
import static org.atsign.client.api.AtEvents.AtEventType.statsNotification;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class SimpleAtEventBusTest {

  @Test
  void testGetEventListenersReturnsExpectedList() {
    ExecutorService executor = mock(ExecutorService.class);
    SimpleAtEventBus bus = new SimpleAtEventBus(executor);

    assertThat(bus.eventListeners, is(anEmptyMap()));

    AtEvents.AtEventListener listener1 = mock(AtEvents.AtEventListener.class);
    bus.addEventListener(listener1, Set.of(statsNotification));

    assertThat(bus.eventListeners,
               equalTo(Collections.singletonMap(listener1, Set.of(statsNotification))));

    AtEvents.AtEventListener listener2 = mock(AtEvents.AtEventListener.class);
    bus.addEventListener(listener2, Set.of(AtEvents.AtEventType.values()));

    assertThat(bus.eventListeners.size(), equalTo(2));

    bus.addEventListener(listener1, Set.of(AtEvents.AtEventType.decryptedUpdateNotification));

    assertThat(bus.eventListeners.size(), equalTo(2));

    bus.removeEventListener(listener1);

    assertThat(bus.eventListeners,
               equalTo(Collections.singletonMap(listener2, Set.of(AtEvents.AtEventType.values()))));

    bus.removeEventListener(listener2);

    assertThat(bus.eventListeners, is(anEmptyMap()));

  }

  @Test
  void testPublishEventInvokesExecutorAndRunningThoseRunnablesInvokesListeners() {

    ExecutorService executor = mock(ExecutorService.class);
    SimpleAtEventBus bus = new SimpleAtEventBus(executor);
    AtEvents.AtEventListener listener1 = mock(AtEvents.AtEventListener.class);
    bus.addEventListener(listener1, Set.of(statsNotification));
    AtEvents.AtEventListener listener2 = mock(AtEvents.AtEventListener.class);
    bus.addEventListener(listener2, Set.of(AtEvents.AtEventType.values()));
    doThrow(new RuntimeException("deliberate")).when(listener2)
        .handleEvent(eq(decryptedUpdateNotification), ArgumentMatchers.any(Map.class));
    AtEvents.AtEventListener listener3 = mock(AtEvents.AtEventListener.class);
    bus.addEventListener(listener3, Set.of(decryptedUpdateNotification));

    HashMap<String, Object> map = new HashMap<>();
    map.put("somedata", "somevalue");
    bus.publishEvent(statsNotification, map);
    bus.publishEvent(decryptedUpdateNotification, map);

    ArgumentCaptor<Runnable> captor = ArgumentCaptor.forClass(Runnable.class);
    verify(executor, times(2)).submit(captor.capture());
    captor.getAllValues().forEach(Runnable::run);

    verify(listener1).handleEvent(eq(statsNotification), eq(map));
    verifyNoMoreInteractions(listener1);

    verify(listener2).handleEvent(eq(statsNotification), eq(map));
    verify(listener2).handleEvent(eq(decryptedUpdateNotification), eq(map));

    verify(listener3).handleEvent(eq(decryptedUpdateNotification), eq(map));
    verifyNoMoreInteractions(listener3);

  }
}
