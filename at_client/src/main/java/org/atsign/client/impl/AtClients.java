package org.atsign.client.impl;

import org.atsign.client.api.AtClient;
import org.atsign.client.api.AtCommandExecutor;
import org.atsign.client.api.AtKeys;
import org.atsign.client.api.AtSign;
import org.atsign.client.impl.common.SimpleAtEventBus;
import org.atsign.client.impl.exceptions.AtException;

import lombok.Builder;

/**
 * Utility methods / builders for instantiating {@link AtClient} implementations
 * that are included in this library.
 * Example usage:
 *
 * <pre>
 * AtClientBuilder builder = AtClients.builder()
 *     .url("vip.ve.atsign.zone:64")
 *     .atSign(createAtSign("colin"))
 *     .keys(...);
 *
 * try (AtClient client = builder.build()) {
 *     client.startMonitor();
 *     client.put(...);
 *     client.get(...);
 * }
 * </pre>
 *
 */
public class AtClients {

  @Builder(builderClassName = "AtClientBuilder")
  public static AtClient createAtClient(String url,
                                        AtSign atSign,
                                        AtKeys keys,
                                        ReconnectStrategy reconnect,
                                        Boolean isVerbose)
      throws AtException {

    AtCommandExecutor executor = AtCommandExecutors.builder()
        .url(url)
        .keys(keys)
        .atSign(atSign)
        .reconnect(reconnect)
        .isVerbose(isVerbose)
        .build();

    SimpleAtEventBus eventBus = new SimpleAtEventBus();

    return AtClientImpl.builder()
        .atSign(atSign)
        .keys(keys)
        .executor(executor)
        .eventBus(eventBus)
        .build();
  }
}
