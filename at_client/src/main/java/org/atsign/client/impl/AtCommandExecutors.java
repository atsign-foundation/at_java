package org.atsign.client.impl;


import java.util.function.Consumer;

import org.atsign.client.api.AtCommandExecutor;
import org.atsign.client.api.AtKeys;
import org.atsign.client.api.AtSign;
import org.atsign.client.impl.commands.AuthenticationCommands;
import org.atsign.client.impl.common.SimpleReconnectStrategy;
import org.atsign.client.impl.exceptions.AtException;
import org.atsign.client.impl.netty.NettyAtCommandExecutor;

import lombok.Builder;

/**
 * Utility methods / builders for instantiating {@link AtCommandExecutor} implementations
 * that are included in this library.
 * Example usage:
 *
 * <pre>
 * AtCommandExecutorBuilder builder = AtCommandExecutors.builder()
 *     .url("vip.ve.atsign.zone:64")
 *     .atSign(createAtSign("colin"))
 *     .keys(...);
 *
 * try (AtCommandExecutor executor = builder.build()) {
 *     client.sendSync(...);
 * }
 * </pre>
 *
 * <b>NOTE:</b> If the url is prefixed with proxy (e.g. proxy:host:port) then the builder
 * will automatically attempt to connect to an At Server at host:port.
 * <b>NOTE:</b> If atSign and keys are provided then the builder
 * will automatically configure the {@link AtCommandExecutor} to authenticate with PKAM.
 * <b>NOTE:</b> If reconnect is not set then the builder will default to a
 * {@link SimpleReconnectStrategy}
 */
public class AtCommandExecutors {

  @Builder(builderClassName = "AtCommandExecutorBuilder")
  public static AtCommandExecutor createCommandExecutor(String url,
                                                        AtSign atSign,
                                                        AtKeys keys,
                                                        ReconnectStrategy reconnect,
                                                        Boolean isVerbose)
      throws AtException {

    return NettyAtCommandExecutor.builder()
        .endpoint(AtEndpointSuppliers.builder().url(url).atSign(atSign).build())
        .isVerbose(isVerbose)
        .reconnect(defaultIfNotSet(reconnect))
        .onReady(createOnReady(atSign, keys))
        .build();
  }

  private static Consumer<AtCommandExecutor> createOnReady(AtSign atSign, AtKeys keys) {
    Consumer<AtCommandExecutor> onReady;
    if (atSign != null && keys != null) {
      onReady = AuthenticationCommands.pkamAuthenticator(atSign, keys);
    } else {
      onReady = c -> {
      };
    }
    return onReady;
  }

  private static ReconnectStrategy defaultIfNotSet(ReconnectStrategy reconnect) {
    return reconnect != null ? reconnect : SimpleReconnectStrategy.builder().build();
  }

}
