package org.atsign.client.api.impl.clients;


import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.atsign.client.api.AtClient;
import org.atsign.client.api.AtKeys;
import org.atsign.client.api.impl.events.SimpleAtEventBus;
import org.atsign.client.connection.api.AtClientConnection;
import org.atsign.client.connection.api.AtEndpointSupplier;
import org.atsign.client.connection.api.ReconnectStrategy;
import org.atsign.client.connection.common.SimpleReconnectStrategy;
import org.atsign.client.connection.netty.NettyAtClientConnection;
import org.atsign.client.connection.netty.NettyAtEndpointSupplier;
import org.atsign.client.connection.protocol.Authentication;
import org.atsign.common.AtException;
import org.atsign.common.AtSign;

import lombok.Builder;

/**
 * Utilities for instantiating AtClient implementations
 */
public class AtClients {

  @Builder(builderClassName = "AtClientsBuilder", builderMethodName = "builder")
  public static AtClient createAtClient(String url,
                                        AtSign atSign,
                                        AtKeys keys,
                                        ReconnectStrategy reconnect,
                                        Boolean isVerbose)
      throws AtException {

    // Netty based connection implementation
    AtClientConnection connection = NettyAtClientConnection.builder()
        .endpoint(createEndpointSupplier(url, atSign))
        .isVerbose(isVerbose)
        .reconnect(reconnect != null ? reconnect : SimpleReconnectStrategy.builder().build())
        .onReady(Authentication.pkamAuthenticator(atSign, keys))
        .build();

    return DefaultAtClientImpl.builder()
        .atSign(atSign)
        .keys(keys)
        .connection(connection)
        .eventBus(new SimpleAtEventBus())
        .build();
  }

  private static AtEndpointSupplier createEndpointSupplier(String url, AtSign atSign) {
    Matcher matcher = Pattern.compile("proxy:(.+)").matcher(url);
    if (matcher.matches()) {
      return () -> matcher.group(1);
    } else {
      return NettyAtEndpointSupplier.builder()
          .rootUrl(url)
          .atsign(atSign)
          .build();
    }
  }
}
