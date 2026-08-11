package org.atsign.client.impl;

import static org.atsign.client.impl.common.Preconditions.checkNotNull;

import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.atsign.client.api.AtSign;
import org.atsign.client.impl.common.SimpleReconnectStrategy;
import org.atsign.client.impl.netty.NettyAtEndpointSupplier;

import lombok.Builder;

/**
 * Utility methods / builders for instantiating {@link AtEndpointSupplier} implementations
 * that are included in this library.
 * Example usage:
 *
 * <pre>
 * AtEndpointSupplierBuilder builder = AtEndpointSupplier.builder()
 *     .url("vip.ve.atsign.zone:64")
 *     .atSign(AtSign.of("colin"));
 *
 * try (AtEndpointSupplier endpoint = builder.build()) {
 *   return endpoint.get();
 * }
 * </pre>
 *
 * <b>NOTE:</b> If the url is proxy:host:port then the {@link AtEndpointSupplier} will always be
 * host:port.
 * <b>NOTE:</b> If the url is host:port then the {@link AtEndpointSupplier} will assume that is the
 * endpoint
 * for the root server, and will resolve the {@link AtSign} endpoint via the root server.
 * <b>NOTE:</b> If port is not specified then it will default to {@link #ROOT_SERVER_PORT}
 * {@link SimpleReconnectStrategy}
 */
public class AtEndpointSuppliers {

  private static final Pattern PATTERN_PROXY_URL = Pattern.compile("proxy:(.+)");

  private static final Pattern PATTERN_ROOT_URL = Pattern.compile("([^:]+)(?::(\\d+))?");

  public static final String DEFAULT_ROOT_URL = "root.atsign.org:64";

  public static final int ROOT_SERVER_PORT = 64;

  @Builder(builderClassName = "AtEndpointSuppliersBuilder")
  public static AtEndpointSupplier createEndpointSupplier(String url, AtSign atSign) {

    url = url != null ? url : DEFAULT_ROOT_URL;

    Matcher proxyMatcher = PATTERN_PROXY_URL.matcher(url);
    if (proxyMatcher.matches()) {
      return () -> proxyMatcher.group(1);
    }

    checkNotNull(atSign, "atSign not set");
    Matcher rootMatcher = PATTERN_ROOT_URL.matcher(url);
    if (rootMatcher.matches()) {
      checkNotNull(atSign, "atSign must be set to resolve at server endpoint from " + url);
      String hostname = rootMatcher.group(1);
      String port = rootMatcher.group(2);
      return NettyAtEndpointSupplier.builder()
          .rootUrl(hostname + ":" + (port != null ? port : ROOT_SERVER_PORT))
          .atsign(atSign)
          .timeoutMillis(TimeUnit.SECONDS.toMillis(5))
          .awaitReadyMillis(TimeUnit.SECONDS.toMillis(5))
          .reconnect(SimpleReconnectStrategy.builder().maxReconnectRetries(3).build())
          .build();
    }

    throw new IllegalArgumentException("url is invalid");
  }

  /**
   * A builder for instantiating {@link AtEndpointSupplier} implementations that are included in
   * this library.
   *
   * <pre>
   *
   * AtEndpointSuppliers.builder()
   *   .url(...)     // the url for the root server or a proxy (optional)
   *   .atSign(...)  // the AtSign that this supplier will resolve if using root server
   *   .build();
   * }
   * </pre>
   *
   * If <b>url</b> is not set then the builder will default to {@link #DEFAULT_ROOT_URL}.
   */
  public static class AtEndpointSuppliersBuilder {
    // required for javadoc
  }

  public static boolean isProxyUrl(String s) {
    return s != null && PATTERN_PROXY_URL.matcher(s).matches();
  }
}
