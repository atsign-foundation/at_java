package org.atsign.client.impl.common;

import org.atsign.client.impl.AtEndpointSupplier;

/**
 * A strategy for controlling re-connection behavior for
 * {@link org.atsign.client.api.AtCommandExecutor} implementations where there is
 * a connection.
 */
public interface ReconnectStrategy {

  /**
   * Invoked by the {@link org.atsign.client.api.AtCommandExecutor} implementation to determine
   * whether it should attempt to reconnect when it fails to connect or becomes disconnected.
   *
   * @return true if a {@link org.atsign.client.api.AtCommandExecutor} should ever attempt to
   *         reconnect
   */
  boolean isReconnectSupported();

  /**
   * Invoked by the {@link org.atsign.client.api.AtCommandExecutor} if the implementation
   * fails to connect. Can be used to maintain state / context that influences behavior.
   *
   * @param ex A throwable that indicated the connection failure.
   */
  void onConnectFailure(Throwable ex);

  /**
   * Invoked by the {@link org.atsign.client.api.AtCommandExecutor} if the implementation
   * successfully connects. Can be used to maintain state / context that influences behavior.
   *
   */
  void onConnect();

  /**
   * Invoked by the {@link org.atsign.client.api.AtCommandExecutor} if the implementation
   * becomes disconnected. i.e. It was connect and now it is not connected.
   * Can be used to maintain state / context that influences behavior.
   *
   * @param ex A throwable that indicated the disconnection.
   */
  void onDisconnect(Throwable ex);

  /**
   * Invoked by the {@link org.atsign.client.api.AtCommandExecutor} implementation to determine
   * how long it should pause for before attempting ro reconnect.
   *
   * @return number of millis seconds to pause (less than 1 = no pause).
   */
  long getReconnectPauseMillis();

  /**
   * Invoked by the {@link org.atsign.client.api.AtCommandExecutor} implementation to determine
   * whether it should re-resolve the connection endpoint (typically via {@link AtEndpointSupplier}
   * prior to a reconnect attempt.
   *
   * @return true if a {@link org.atsign.client.api.AtCommandExecutor} should re-resolve.
   */
  boolean isReresolveEndpoint();

  /**
   * A strategy where no reconnection is required
   */
  ReconnectStrategy NONE = new ReconnectStrategy() {

    @Override
    public boolean isReconnectSupported() {
      return false;
    }

    @Override
    public void onConnectFailure(Throwable ex) {}

    @Override
    public void onConnect() {}

    @Override
    public void onDisconnect(Throwable ex) {}

    @Override
    public long getReconnectPauseMillis() {
      return 0;
    }

    @Override
    public boolean isReresolveEndpoint() {
      return false;
    }
  };

}
