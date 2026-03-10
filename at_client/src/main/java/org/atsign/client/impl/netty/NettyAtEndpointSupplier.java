package org.atsign.client.impl.netty;

import static org.atsign.client.impl.common.Preconditions.checkNotNull;


import javax.net.ssl.SSLContext;

import org.atsign.client.impl.AtEndpointSupplier;
import org.atsign.client.api.AtSign;
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
  public NettyAtEndpointSupplier(String rootUrl, AtSign atsign, Long timeoutMillis, SSLContext sslContext) {
    this.atsign = checkNotNull(atsign, "atSign not set");
    String hostAndPort = checkNotNull(rootUrl, "rootUrl not set");
    this.executorBuilder = NettyAtCommandExecutor.builder()
        .endpoint(() -> hostAndPort)
        .timeoutMillis(timeoutMillis)
        .sslContext(sslContext);
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
