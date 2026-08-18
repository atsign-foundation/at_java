package org.atsign.client.impl;

import static org.atsign.client.impl.common.Preconditions.checkNotNull;

import java.io.File;
import java.util.Map;
import java.util.function.Consumer;

import org.atsign.client.api.AtClient;
import org.atsign.client.api.AtCommandExecutor;
import org.atsign.client.api.AtCommandExecutorContext;
import org.atsign.client.api.AtKeys;
import org.atsign.client.api.AtSign;
import org.atsign.client.impl.commands.MonitorOptions;
import org.atsign.client.impl.commands.Notifications;
import org.atsign.client.impl.common.ReconnectStrategy;
import org.atsign.client.impl.common.SimpleAtEventBus;
import org.atsign.client.impl.common.SimpleReconnectStrategy;
import org.atsign.client.impl.exceptions.AtClientConfigException;
import org.atsign.client.impl.exceptions.AtException;
import org.atsign.client.impl.util.KeysUtils;

import lombok.Builder;
import lombok.extern.slf4j.Slf4j;

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
@Slf4j
public class AtClients {

  @Builder(builderClassName = "AtClientBuilder")
  public static AtClient createAtClient(String url,
                                        AtSign atSign,
                                        AtKeys keys,
                                        String keysPath,
                                        boolean withMonitoring,
                                        MonitorOptions monitorOptions,
                                        Map<String, Object> config,
                                        Long timeoutMillis,
                                        Long awaitReadyMillis,
                                        ReconnectStrategy reconnect,
                                        Integer queueLimit,
                                        Boolean isVerbose)
      throws AtException {

    checkNotNull(atSign, "atSign not set");
    keys = keys != null ? keys : loadKeys(keysPath, atSign);

    SimpleAtEventBus eventBus = new SimpleAtEventBus();

    if (monitorOptions == null) {
      monitorOptions = MonitorOptions.builder().build();
    }

    AtCommandExecutorContext context = createContext(atSign, keys, config);

    Consumer<AtCommandExecutor> onReady = null;
    if (withMonitoring) {
      Notifications.EventBusBridge eventBusBridge = new Notifications.EventBusBridge(eventBus, atSign, monitorOptions);
      onReady = createMonitoringOnReady(context, monitorOptions, eventBusBridge);
    }

    AtCommandExecutor executor = AtCommandExecutors.builder()
        .url(url)
        .context(context)
        .onReady(onReady)
        .timeoutMillis(timeoutMillis)
        .awaitReadyMillis(awaitReadyMillis)
        .reconnect(reconnect)
        .queueLimit(queueLimit)
        .isVerbose(isVerbose)
        .build();

    return AtClientImpl.builder()
        .context(context)
        .withMonitoring(withMonitoring)
        .monitorOptions(monitorOptions)
        .executor(executor)
        .eventBus(eventBus)
        .build();
  }

  /**
   * The one context for the whole connection. The executor's {@code onReady} sequence and every
   * command the client issues both read from it, so they share one {@code from:} challenge and one
   * client config.
   *
   * <p>
   * The config is enriched here — {@code clientId} plus the {@code client-config.properties} entries
   * — and enriched exactly once, because a second call would mint a second {@code clientId}.
   *
   * <p>
   * Package-private so a test can drive it together with {@link #createMonitoringOnReady}.
   */
  static AtCommandExecutorContext createContext(AtSign atSign, AtKeys keys, Map<String, Object> config) {
    return new AtCommandExecutorContext(atSign, keys, AtCommandExecutors.createClientConfig(config));
  }

  /**
   * The {@code onReady} sequence for a client built with monitoring: authenticate with PKAM using the
   * connection's context, then send the {@code monitor} command. Package-private so that it can be
   * driven with a stubbed {@link AtCommandExecutor} in tests.
   */
  static Consumer<AtCommandExecutor> createMonitoringOnReady(AtCommandExecutorContext context,
                                                             MonitorOptions monitorOptions,
                                                             Consumer<String> consumer) {
    return Notifications.monitor(context, monitorOptions, consumer);
  }

  private static AtKeys loadKeys(String path, AtSign atSign) throws AtClientConfigException {
    if (path == null) {
      return KeysUtils.loadKeys(atSign);
    }
    File f = new File(path);
    if (!f.exists()) {
      throw new AtClientConfigException(path + " does not exist");
    }
    if (f.isDirectory()) {
      return KeysUtils.loadKeys(KeysUtils.getKeysFile(atSign, path));
    }
    return KeysUtils.loadKeys(f);
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
   *   .keysPath(...)      // the location for the AtKeys that this client will use (optional)
   *   .withMonitoring()   // if true then monitoring is automatically started
   *   .config(...)        // the config map that will be passed during authentication (optional)
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
   * If <b>keysPath</b> is set (and keys is not set) then the builder will attempt to load keys
   * from the path value. If the provided value is not a directory it will simply load the file,
   * otherwise it will look for a atKeys file for the atSign in the path.
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
