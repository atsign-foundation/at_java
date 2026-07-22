package org.atsign.client.impl;


import lombok.Builder;
import lombok.extern.slf4j.Slf4j;
import org.atsign.client.api.AtCommandExecutor;
import org.atsign.client.api.AtCommandExecutorContext;
import org.atsign.client.api.AtKeys;
import org.atsign.client.api.AtSign;
import org.atsign.client.impl.commands.AuthenticationCommands;
import org.atsign.client.impl.common.ReconnectStrategy;
import org.atsign.client.impl.common.SimpleReconnectStrategy;
import org.atsign.client.impl.exceptions.AtException;
import org.atsign.client.impl.netty.NettyAtCommandExecutor;

import java.io.InputStream;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import static org.atsign.client.impl.common.Preconditions.checkNotNull;

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
 * <b>NOTE:</b> If an atSign is provided then the builder issues {@code from:@atSign} as the first
 * command once connected (so proxies / gateways can route the connection); if keys are also
 * provided it then authenticates the {@link AtCommandExecutor} with PKAM, reusing that
 * {@code from:}'s challenge.
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
                                                        Consumer<AtCommandExecutor> onReady,
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

    // the context is closed over by the onReady consumers the builder wires (see createOnReady); the
    // command executor itself stays pure transport and knows nothing about it
    AtCommandExecutorContext context = new AtCommandExecutorContext(atSign, keys, createClientConfig(config));

    return NettyAtCommandExecutor.builder()
        .endpoint(AtEndpointSuppliers.builder().url(url).atSign(atSign).build())
        .isVerbose(isVerbose)
        .timeoutMillis(defaultIfNotSet(timeoutMillis, DEFAULT_TIMEOUT_MILLIS))
        .awaitReadyMillis(defaultIfNotSet(awaitReadyMillis, DEFAULT_TIMEOUT_MILLIS))
        .reconnect(defaultIfNotSet(reconnect, SimpleReconnectStrategy.builder().build()))
        .queueLimit(queueLimit)
        .onReady(defaultIfNotSet(onReady, createOnReady(context)))
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

  /**
   * The default protocol for a newly-ready connection. A connection with no atSign (e.g. one talking
   * to the atDirectory / root server) sends nothing. Every connection that has an atSign issues
   * {@code from:@atSign} first so that proxies / gateways can route it; if keys are also present it
   * then authenticates with PKAM, reusing the challenge from that initial {@code from:}.
   */
  private static Consumer<AtCommandExecutor> createOnReady(AtCommandExecutorContext context) {
    if (context.getAtSign() == null) {
      return c -> {
      };
    }
    Consumer<AtCommandExecutor> onReady = AuthenticationCommands.sendFrom(context);
    if (context.getKeys() != null) {
      onReady = onReady.andThen(AuthenticationCommands.pkamAuthenticator(context));
    }
    return onReady;
  }

  private static <T> T defaultIfNotSet(T value, T defaultValue) {
    return value != null ? value : defaultValue;
  }

  private static long defaultIfNotSet(Long l, long defaultValue) {
    return l != null ? l : defaultValue;
  }
}
