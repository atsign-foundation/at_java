package org.atsign.client.impl.common;

import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

class SimpleReconnectStrategyTest {

  @Test
  void testIsReconnectSupportedWhenMaxRetriesIsForever() {
    SimpleReconnectStrategy strategy = SimpleReconnectStrategy.builder()
        .maxReconnectRetries(SimpleReconnectStrategy.RECONNECT_RETRY_FOREVER)
        .build();

    assertThat(strategy.isReconnectSupported(), is(true));
    RuntimeException ex = new RuntimeException("deliberate");
    for (int i = 0; i < 1000; i++) {
      strategy.onConnectFailure(ex);
      assertThat(strategy.isReconnectSupported(), is(true));
    }
  }

  @Test
  void testIsReconnectSupportedWhenMaxRetriesIsNotForever() {
    SimpleReconnectStrategy strategy = SimpleReconnectStrategy.builder()
        .maxReconnectRetries(3)
        .build();

    // connect failure count < max
    strategy.onConnectFailure(new RuntimeException());
    strategy.onConnectFailure(new RuntimeException());
    assertThat(strategy.isReconnectSupported(), is(true));

    // connect failure count = max
    strategy.onConnectFailure(new RuntimeException());
    assertThat(strategy.isReconnectSupported(), is(true));

    // connect failure count > max
    strategy.onConnectFailure(new RuntimeException());
    assertThat(strategy.isReconnectSupported(), is(false));
  }

  @Test
  void testIsReconnectSupportedAfterConnect() {
    SimpleReconnectStrategy strategy = SimpleReconnectStrategy.builder()
        .maxReconnectRetries(3)
        .build();

    strategy.onConnectFailure(new RuntimeException());
    strategy.onConnectFailure(new RuntimeException());
    strategy.onConnectFailure(new RuntimeException());
    strategy.onConnect();
    strategy.onDisconnect(new RuntimeException());

    strategy.onConnectFailure(new RuntimeException());
    assertThat(strategy.isReconnectSupported(), is(true));
  }

  @Test
  void testGetReconnectPauseMillis() {
    SimpleReconnectStrategy strategy = SimpleReconnectStrategy.builder()
        .maxReconnectRetries(5)
        .reconnectPauseMillis(2000L)
        .build();

    strategy.onConnectFailure(new RuntimeException());
    strategy.onConnect();

    // first disconnect after connect
    assertThat(strategy.getReconnectPauseMillis(), is(0L));

    // subsequent connection failures
    strategy.onConnectFailure(new RuntimeException());
    assertThat(strategy.getReconnectPauseMillis(), is(2000L));
    strategy.onConnectFailure(new RuntimeException());
    assertThat(strategy.getReconnectPauseMillis(), is(2000L));

    strategy.onConnect();

    // first disconnect after connect
    assertThat(strategy.getReconnectPauseMillis(), is(0L));
  }

  @Test
  void testGetReconnectPauseMillisDefault() {
    SimpleReconnectStrategy strategy = SimpleReconnectStrategy.builder()
        .maxReconnectRetries(5)
        .build();

    strategy.onConnectFailure(new RuntimeException());
    strategy.onConnect();

    // first disconnect after connect
    assertThat(strategy.getReconnectPauseMillis(), is(0L));

    // subsequent connection failures
    strategy.onConnectFailure(new RuntimeException());
    assertThat(strategy.getReconnectPauseMillis(), is(1000L));
  }

  @Test
  void testIsReresolveEndpointsEveryTime() {
    SimpleReconnectStrategy strategy = SimpleReconnectStrategy.builder()
        .maxReconnectRetries(5)
        .resolveEndpointFrequency(1)
        .build();

    // first disconnect
    strategy.onDisconnect(new RuntimeException());
    assertThat(strategy.isReresolveEndpoint(), is(false));

    // subsequent connection failures
    strategy.onConnectFailure(new RuntimeException());
    assertThat(strategy.isReresolveEndpoint(), is(true));
    strategy.onConnectFailure(new RuntimeException());
    assertThat(strategy.isReresolveEndpoint(), is(true));
  }


  @Test
  void testIsReresolveEndpointsEverOtherTime() {
    SimpleReconnectStrategy strategy = SimpleReconnectStrategy.builder()
        .maxReconnectRetries(5)
        .resolveEndpointFrequency(2)
        .build();

    // first disconnect
    strategy.onDisconnect(new RuntimeException());
    assertThat(strategy.isReresolveEndpoint(), is(false));

    // subsequent connection failures
    strategy.onConnectFailure(new RuntimeException());
    assertThat(strategy.isReresolveEndpoint(), is(false));
    strategy.onConnectFailure(new RuntimeException());
    assertThat(strategy.isReresolveEndpoint(), is(true));
    strategy.onConnectFailure(new RuntimeException());
    assertThat(strategy.isReresolveEndpoint(), is(false));
  }


  @Test
  void testIsReresolveEndpointsDefaultNeverReresolves() {
    SimpleReconnectStrategy strategy = SimpleReconnectStrategy.builder()
        .maxReconnectRetries(5)
        .build();
    strategy.onDisconnect(new RuntimeException());
    assertThat(strategy.isReresolveEndpoint(), is(false));
    strategy.onConnectFailure(new RuntimeException());
    assertThat(strategy.isReresolveEndpoint(), is(false));
    strategy.onConnectFailure(new RuntimeException());
    assertThat(strategy.isReresolveEndpoint(), is(false));
    strategy.onConnectFailure(new RuntimeException());
    assertThat(strategy.isReresolveEndpoint(), is(false));
  }

}
