package org.atsign.client.connection.netty;

import org.atsign.client.connection.api.AtEndpointSupplier;
import org.atsign.common.AtSign;
import org.atsign.common.exceptions.AtSecondaryNotFoundException;

import lombok.Builder;

import javax.net.ssl.SSLContext;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.atsign.client.util.Preconditions.checkNotNull;

/**
 * An implementation that uses a {@link NettyAtClientConnection} to connect to the
 * root / directory server and resolve the endpoint for a specific {@link AtSign}
 */
public class NettyAtEndpointSupplier implements AtEndpointSupplier {

  private final AtSign atsign;

  private final NettyAtClientConnection.NettyAtClientConnectionBuilder connectionBuilder;

  @Builder
  public NettyAtEndpointSupplier(String rootUrl, AtSign atsign, Long timeoutMillis, SSLContext sslContext) {
    this.atsign = checkNotNull(atsign, "atSign not set");
    String hostAndPort = defaultPort(checkNotNull(rootUrl, "rootUrl not set"));
    this.connectionBuilder = NettyAtClientConnection.builder()
        .endpoint(() -> hostAndPort)
        .timeoutMillis(timeoutMillis)
        .sslContext(sslContext);
  }

  @Override
  public String get() throws AtSecondaryNotFoundException {
    try (NettyAtClientConnection connection = connectionBuilder.build()) {
      return checkNotBlank(connection.sendSync(atsign.withoutPrefix()));
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

  private static String defaultPort(String hostAndPort) {
    Matcher matcher = Pattern.compile("([^:]+):(\\d+)").matcher(hostAndPort);
    if (matcher.matches()) {
      return hostAndPort;
    } else {
      return hostAndPort + ":" + 64;
    }
  }
}
