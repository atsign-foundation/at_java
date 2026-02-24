package org.atsign.client.connection.api;

/**
 * A strategy for controlling re-connection behavior
 */
public interface ReconnectStrategy {

  boolean isReconnectSupported();

  void onConnectFailure(Throwable ex);

  void onConnect();

  void onDisconnect(Throwable ex);

  long getReconnectPauseMillis();

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
