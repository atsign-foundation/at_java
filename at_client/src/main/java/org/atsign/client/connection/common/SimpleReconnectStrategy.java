package org.atsign.client.connection.common;

import lombok.Builder;
import org.atsign.client.connection.api.ReconnectStrategy;

import java.util.concurrent.TimeUnit;

/**
 * A default implementation based on a number of reconnection retries with a simple
 * back-off strategy and re-resolve strategy.
 */
public class SimpleReconnectStrategy implements ReconnectStrategy {

  public static final long RECONNECT_RETRY_FOREVER = 0;

  public static final long DEFAULT_RECONNECT_PAUSE_MILLIS = TimeUnit.SECONDS.toMillis(1);

  private final long maxReconnectRetries;
  private final int resolveEndpointFrequency;
  private final long reconnectPauseMillis;

  private long connectFailureCount;

  @Builder
  public SimpleReconnectStrategy(long maxReconnectRetries, int resolveEndpointFrequency, long reconnectPauseMillis) {
    this.maxReconnectRetries = maxReconnectRetries;
    this.resolveEndpointFrequency = resolveEndpointFrequency;
    this.reconnectPauseMillis = reconnectPauseMillis;
  }

  @Override
  public boolean isReconnectSupported() {
    return maxReconnectRetries == RECONNECT_RETRY_FOREVER || connectFailureCount <= maxReconnectRetries;
  }

  @Override
  public void onConnectFailure(Throwable ex) {
    connectFailureCount++;
  }

  @Override
  public void onConnect() {
    connectFailureCount = 0;
  }

  @Override
  public void onDisconnect(Throwable ex) {}

  @Override
  public long getReconnectPauseMillis() {
    if (connectFailureCount == 0) {
      return 0;
    } else {
      return reconnectPauseMillis > 0 ? reconnectPauseMillis : DEFAULT_RECONNECT_PAUSE_MILLIS;
    }
  }

  @Override
  public boolean isReresolveEndpoint() {
    if (connectFailureCount == 0) {
      return false;
    } else {
      return resolveEndpointFrequency > 0 && (connectFailureCount % resolveEndpointFrequency) == 0;
    }
  }
}
