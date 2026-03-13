package org.atsign.client.impl.netty;

import static org.atsign.client.impl.common.Preconditions.checkNotNull;

import javax.net.ssl.SSLContext;

import org.atsign.client.api.AtCommandExecutor;
import org.atsign.client.api.AtSign;
import org.atsign.client.impl.AtEndpointSupplier;
import org.atsign.client.impl.common.ReconnectStrategy;
import org.atsign.client.impl.exceptions.AtSecondaryNotFoundException;
import org.atsign.client.impl.netty.NettyAtCommandExecutor.NettyAtCommandExecutorBuilder;

import lombok.Builder;

/**
 * An implementation that uses a {@link NettyAtCommandExecutor} to connect to the
 * root / directory server and resolve the endpoint for a specific {@link AtSign}
 */
public class NettyAtEndpointSupplier implements AtEndpointSupplier {

  private final AtSign atsign;

  private final NettyAtCommandExecutorBuilder executorBuilder;

  @Builder
  public NettyAtEndpointSupplier(String rootUrl,
                                 AtSign atsign,
                                 Long timeoutMillis,
                                 Long awaitReadyMillis,
                                 ReconnectStrategy reconnect,
                                 SSLContext sslContext) {
    this.atsign = checkNotNull(atsign, "atSign not set");
    String hostAndPort = checkNotNull(rootUrl, "rootUrl not set");
    this.executorBuilder = NettyAtCommandExecutor.builder()
        .endpoint(() -> hostAndPort)
        .timeoutMillis(timeoutMillis)
        .awaitReadyMillis(awaitReadyMillis)
        .reconnect(reconnect)
        .sslContext(sslContext);
  }

  /**
   * A builder for instantiating {@link AtCommandExecutor} implementations that are included in
   * this library. Example usage:
   *
   * <pre>
   *
   * NettyAtEndpointSupplier.builder()
   *   .rootUrl(...)       // the endpoint for the root / directory server
   *   .atSign(...)        // the AtSign to resolve endpoint for
   *   .timeoutMillis()    // timeout after which commands will complete exceptionally (optional)
   *   .awaitReadyMillis() // how long to wait for executor to become ready during build() (optional)
   *   .reconnect()        // a ReconnectStrategy (optional)
   *   .sslContext()       // (optional)
   *   .build();
   * }
   * </pre>
   *
   * If <b>timeoutMillis</b> is not set then builder will default to
   * {@link NettyAtCommandExecutor#DEFAULT_COMMAND_TIMEOUT_MILLIS}.
   * If <b>awaitReadyMillis</b> is not set then the builder will default to
   * {@link NettyAtCommandExecutor#DEFAULT_AWAIT_READY_MILLIS}.
   * If <b>reconnect</b> is not set then the builder will default to a {@link ReconnectStrategy#NONE}
   * which will never retry to connect or reconnect after disconnect.
   */
  public static class NettyAtEndpointSupplierBuilder {
    // required for javadoc
  }

  @Override
  public String get() throws AtSecondaryNotFoundException {
    try (NettyAtCommandExecutor executor = executorBuilder.build()) {
      return checkNotBlank(executor.sendSync(atsign.withoutPrefix()));
    } catch (Exception e) {
      throw new AtSecondaryNotFoundException("unable to resolve the endpoint for " + atsign, e);
    }
  }

  private static String checkNotBlank(String s) throws AtSecondaryNotFoundException {
    if (s == null || s.isBlank() || "null".equals(s)) {
      throw new AtSecondaryNotFoundException("unable to resolve the endpoint");
    }
    return s;
  }
}
