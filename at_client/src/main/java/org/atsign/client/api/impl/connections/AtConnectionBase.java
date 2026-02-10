package org.atsign.client.api.impl.connections;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Scanner;

import javax.net.SocketFactory;
import javax.net.ssl.SSLSocketFactory;

import lombok.extern.slf4j.Slf4j;
import org.atsign.client.api.AtConnection;
import org.atsign.client.api.AtEvents;
import org.atsign.common.AtException;

/**
 * Core implementation of an {@link AtConnection} which uses Java NIO. This
 * class contains the implementation of the socket connect / disconnect,
 * writing atprotocol commands and reading atprotocol responses.
 * If the socket becomes disconnected then it will be re-connected as part
 * of sending the next command.
 */
@Slf4j
public abstract class AtConnectionBase implements AtConnection {

  private final String url;

  @Override
  public String getUrl() {
    return url;
  }

  private final String host;

  @Override
  public String getHost() {
    return host;
  }

  private final int port;

  @Override
  public int getPort() {
    return port;
  }

  private Socket socket;

  @Override
  public Socket getSocket() {
    return socket;
  }

  private boolean connected = false;

  @Override
  public boolean isConnected() {
    return connected;
  }

  private final boolean autoReconnect;

  @Override
  public boolean isAutoReconnect() {
    return autoReconnect;
  }

  protected boolean verbose;

  @Override
  public boolean isVerbose() {
    return verbose;
  }

  @Override
  public void setVerbose(boolean verbose) {
    this.verbose = verbose;
  }

  protected final Authenticator authenticator;

  public Authenticator getAuthenticator() {
    return authenticator;
  }

  protected PrintWriter socketWriter;
  protected Scanner socketScanner;

  protected final AtEvents.AtEventBus eventBus;

  public AtConnectionBase(AtEvents.AtEventBus eventBus,
                          String url,
                          AtConnection.Authenticator authenticator,
                          boolean autoReconnect,
                          boolean verbose) {
    this.eventBus = eventBus;
    this.url = url;
    this.host = url.split(":")[0];
    this.port = Integer.parseInt(url.split(":")[1]);
    this.autoReconnect = autoReconnect;
    this.verbose = verbose;
    this.authenticator = authenticator;
  }

  @Override
  public synchronized void disconnect() {
    if (!isConnected()) {
      return;
    }
    connected = false;
    try {
      log.debug("disconnecting from {}:{}", host, port);
      socket.close();
      socketScanner.close();
      socketWriter.close();
      socket.shutdownInput();
      socket.shutdownOutput();
    } catch (Exception ignore) {
    }
  }

  @Override
  public synchronized void connect() throws IOException, AtException {
    if (isConnected()) {
      return;
    }
    SocketFactory sf = SSLSocketFactory.getDefault();
    log.debug("connecting to {}:{}...", host, port);
    this.socket = sf.createSocket(host, port);
    this.socketWriter = new PrintWriter(socket.getOutputStream());
    this.socketScanner = new Scanner(socket.getInputStream());
    log.debug("connected to {}:{}", host, port);

    if (authenticator != null) {
      authenticator.authenticate(this);
    }
    connected = true;
  }

  protected abstract String parseRawResponse(String rawResponse) throws IOException;

  @Override
  public final synchronized String executeCommand(String command) throws IOException {
    return executeCommand(command, autoReconnect, true);
  }

  protected synchronized String executeCommand(String command, boolean retryOnException, boolean readTheResponse)
      throws IOException {
    if (socket.isClosed()) {
      throw new IOException("executeCommand failed: socket is closed");
    }
    try {
      if (!command.endsWith("\n")) {
        command = command + "\n";
      }
      socketWriter.write(command);
      socketWriter.flush();

      if (verbose) {
        log.info("SENT: {}", command);
      }

      if (readTheResponse) {
        // Responses are always terminated by newline
        String rawResponse = socketScanner.nextLine();
        if (verbose) {
          log.info("RCVD: {}", rawResponse);
        }

        return parseRawResponse(rawResponse);
      } else {
        return "";
      }
    } catch (Exception first) {
      disconnect();

      if (retryOnException) {
        log.error("Caught exception {} : reconnecting", first.toString());
        try {
          connect();
          return executeCommand(command, false, true);
        } catch (Exception second) {
          log.error("failed on retry", second);
          throw new IOException("Failed to reconnect after original exception " + first + " : ", second);
        }
      } else {
        connected = false;

        throw new IOException(first);
      }
    }
  }
}
