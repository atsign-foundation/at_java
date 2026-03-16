package org.atsign.client.impl;

import static org.atsign.client.impl.common.Preconditions.checkNotNull;

import org.atsign.client.api.AtClient;
import org.atsign.client.api.AtCommandExecutor;
import org.atsign.client.api.AtKeys;
import org.atsign.client.api.AtSign;
import org.atsign.client.impl.common.ReconnectStrategy;
import org.atsign.client.impl.common.SimpleAtEventBus;
import org.atsign.client.impl.common.SimpleReconnectStrategy;
import org.atsign.client.impl.exceptions.AtException;
import org.atsign.client.impl.util.KeysUtils;

import lombok.Builder;

/**
 * Utility methods for instantiating {@link AtClient} implementations that are included in this
 * library. Example usage:
 *
 * <pre>
 *
 * try (AtClient client = AtClients.builder().atSign(createAtSign("colin")).build()) {
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
                                        Long timeoutMillis,
                                        Long awaitReadyMillis,
                                        ReconnectStrategy reconnect,
                                        Integer queueLimit,
                                        Boolean isVerbose)
      throws AtException {

    checkNotNull(atSign, "atSign not set");
    keys = keys != null ? keys : KeysUtils.loadKeys(atSign);

    AtCommandExecutor executor = AtCommandExecutors.builder()
        .url(url)
        .atSign(atSign)
        .keys(keys)
        .timeoutMillis(timeoutMillis)
        .awaitReadyMillis(awaitReadyMillis)
        .reconnect(reconnect)
        .queueLimit(queueLimit)
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

  /**
   * A builder for instantiating {@link AtClient} implementations that are included in
   * this library.
   *
   * <pre>
   *
   * AtClients.builder()
   *   .atSign(...)        // the AtSign that this client will authenticate as
   *   .url(...)           // the url for the root server or proxy (optional)
   *   .keys(...)          // the AtKeys that this client will use (optional)
   *   .timeoutMillis()    // timeout after which commands will complete exceptionally (optional)
   *   .awaitReadyMillis() // how long to wait for executor to become ready during build() (optional)
   *   .reconnect()        // a ReconnectStrategy (optional)
   *   .queueLimit()       // number of queued commands that are permitted (optional)
   *   .isVerbose(...)     // true or false (optional)
   *   .build();
   * }
   * </pre>
   *
   * If <b>url</b> is not set then the builder will default to
   * {@link AtEndpointSuppliers#DEFAULT_ROOT_URL}.
   * If <b>keys</b> is not set then the builder will default to attempting to load the keys
   * which correspond to the atSign field in ~/.atsign/keys (or the environment variable
   * / system property {@link KeysUtils#ATSIGN_KEYS_DIR} if set).
   * If <b>timeoutMillis</b> is not set then builder will default to
   * {@link AtCommandExecutors#DEFAULT_TIMEOUT_MILLIS}.
   * If <b>awaitReadyMillis</b> is not set then the builder will default to
   * {@link AtCommandExecutors#DEFAULT_TIMEOUT_MILLIS}.
   * If <b>reconnect</b> is not set then the builder will default to a {@link SimpleReconnectStrategy}
   * with no limit to the retry attempts.
   * If <b>queueLimit</b> is not set then the builder will default zero queued commands
   */
  public static class AtClientBuilder {
    // required for javadoc
  }
}
