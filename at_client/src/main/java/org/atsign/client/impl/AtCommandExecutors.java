package org.atsign.client.impl;


import static org.atsign.client.impl.common.Preconditions.checkNotNull;

import java.io.InputStream;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import org.atsign.client.api.AtCommandExecutor;
import org.atsign.client.api.AtKeys;
import org.atsign.client.api.AtSign;
import org.atsign.client.impl.commands.AuthenticationCommands;
import org.atsign.client.impl.common.ReconnectStrategy;
import org.atsign.client.impl.common.SimpleReconnectStrategy;
import org.atsign.client.impl.exceptions.AtException;
import org.atsign.client.impl.netty.NettyAtCommandExecutor;

import lombok.Builder;
import lombok.extern.slf4j.Slf4j;

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
@Slf4j
public class AtCommandExecutors {

  public static final long DEFAULT_TIMEOUT_MILLIS = TimeUnit.SECONDS.toMillis(5);

  @Builder(builderClassName = "AtCommandExecutorBuilder")
  public static AtCommandExecutor createCommandExecutor(String url,
                                                        AtSign atSign,
                                                        AtKeys keys,
                                                        Map<String, Object> config,
                                                        Long timeoutMillis,
                                                        Long awaitReadyMillis,
                                                        ReconnectStrategy reconnect,
                                                        Integer queueLimit,
                                                        Boolean isVerbose)
      throws AtException {

    if (AtEndpointSuppliers.isProxyUrl(url)) {
      checkNotNull(atSign, "atSign not set");
    }

    return NettyAtCommandExecutor.builder()
        .endpoint(AtEndpointSuppliers.builder().url(url).atSign(atSign).build())
        .isVerbose(isVerbose)
        .timeoutMillis(defaultIfNotSet(timeoutMillis, DEFAULT_TIMEOUT_MILLIS))
        .awaitReadyMillis(defaultIfNotSet(awaitReadyMillis, DEFAULT_TIMEOUT_MILLIS))
        .reconnect(defaultIfNotSet(reconnect))
        .queueLimit(queueLimit)
        .onReady(createOnReady(atSign, keys, createClientConfig(config)))
        .build();
  }

  /**
   * A builder for instantiating {@link AtCommandExecutor} implementations that are included in
   * this library. Example usage:
   *
   * <pre>
   *
   * AtCommandExecutors.builder()
   *   .url(...)           // the url for the root server or proxy (optional)
   *   .atSign(...)        // the AtSign that this client will authenticate as (optional)
   *   .keys(...)          // the AtKeys that this client will use (optional)
   *   .timeoutMillis()    // timeout after which commands will complete exceptionally (optional)
   *   .awaitReadyMillis() // how long to wait for executor to become ready during build() (optional)
   *   .reconnect()        // a ReconnectStrategy (optional)
   *   .isVerbose(...)     // default false
   *   .build();
   * }
   * </pre>
   *
   * If <b>url</b> is not set then the builder will default to
   * {@link AtEndpointSuppliers#DEFAULT_ROOT_URL}.
   * If <b>timeoutMillis</b> is not set then builder will default to
   * {@link AtCommandExecutors#DEFAULT_TIMEOUT_MILLIS}.
   * If <b>awaitReadyMillis</b> is not set then the builder will default to
   * {@link AtCommandExecutors#DEFAULT_TIMEOUT_MILLIS}.
   * If <b>reconnect</b> is not set then the builder will default to a {@link SimpleReconnectStrategy}
   * with no limit to the retry attempts.
   */
  public static class AtCommandExecutorBuilder {
    // required for javadoc
  }

  public static Map<String, Object> createClientConfig(Map<String, Object> config) {
    Map<String, Object> result = new HashMap<>();
    result.put("clientId", UUID.randomUUID());
    Properties properties = new Properties();
    URL resource = AtCommandExecutors.class.getClassLoader().getResource("client-config.properties");
    try (InputStream in = resource.openStream()) {
      properties.load(in);
      properties.forEach((k, v) -> result.put(k.toString(), v.toString().replace("-SNAPSHOT", "")));
    } catch (Exception e) {
      log.warn("unable to load client-config.properties");
    }
    if (config != null) {
      result.putAll(config);
    }
    return result;
  }

  private static Consumer<AtCommandExecutor> createOnReady(AtSign atSign, AtKeys keys, Map<String, Object> config) {
    Consumer<AtCommandExecutor> onReady;
    if (atSign != null && keys != null) {
      onReady = AuthenticationCommands.pkamAuthenticator(atSign, keys, config);
    } else {
      onReady = c -> {
      };
    }
    return onReady;
  }

  private static ReconnectStrategy defaultIfNotSet(ReconnectStrategy reconnect) {
    return reconnect != null ? reconnect : SimpleReconnectStrategy.builder().build();
  }

  private static long defaultIfNotSet(Long l, long defaultValue) {
    return l != null ? l : defaultValue;
  }
}
