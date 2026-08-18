package org.atsign.client.api;

import lombok.*;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static java.util.Collections.emptyMap;
import static java.util.Collections.unmodifiableMap;
import static org.atsign.client.impl.common.Preconditions.checkNotNull;

/**
 * The identity a connection authenticates as (its {@code atSign}, {@code keys} and {@code config})
 * together with the single-use challenge from the {@code from:} that is issued as the first command
 * once the connection is ready.
 *
 * <p>
 * There is one context per connection. {@code AtClients#createAtClient} creates it and passes it to
 * both the executor and the client; {@code AtCommandExecutors#createCommandExecutor} creates one
 * itself when no caller supplies one. The {@code onReady} consumers hold a reference to it, so the
 * {@code from:} sender can retain the challenge and the authentication that follows on the same
 * connection can reuse it instead of issuing a second {@code from:}. The command executor itself
 * only sends and receives, and never sees the context.
 *
 * <p>
 * The identity fields are fixed for the life of the context. The challenge is per-connection state:
 * {@link #setChallenge(String) retained} when the initial {@code from:} completes and
 * {@link #consumeChallenge() consumed} at most once (the server's {@code from:} challenge is
 * single-use). On reconnect the ready sequence re-runs, so a fresh challenge replaces any
 * previous one before it is consumed.
 */
@Value
public class AtCommandExecutorContext {

  /**
   * The atSign the connection authenticates as. Never {@code null}: a connection is built either from
   * a context or from an atSign, and both establish an identity.
   */
  AtSign atSign;

  /**
   * The keys the connection authenticates with, or {@code null} for a connection that is not
   * PKAM-authenticated — one that only issues {@code from:}, or one authenticating with CRAM before
   * any keys exist.
   */
  AtKeys keys;

  /**
   * The client config sent with {@code from:}. Never {@code null} and never modifiable; empty
   * when the connection has none.
   */
  Map<String, Object> config;

  /**
   * Holds the {@code from:} challenge for this connection.
   */
  @Getter(AccessLevel.NONE)
  @EqualsAndHashCode.Exclude
  @ToString.Exclude
  AtomicReference<String> challenge;

  /**
   * A context with no client config, for a connection whose {@code from:} carries no
   * {@code clientConfig} segment.
   *
   * @param atSign the atSign the connection authenticates as
   * @param keys the keys the connection authenticates with, or null if it is not PKAM-authenticated
   */
  public AtCommandExecutorContext(AtSign atSign, AtKeys keys) {
    this(atSign, keys, null);
  }

  /**
   * @param atSign the atSign the connection authenticates as
   * @param keys the keys the connection authenticates with, or null if it is not PKAM-authenticated
   * @param config the client config to send with {@code from:}. Copied, so a later change to the
   *        caller's map cannot alter what this connection sends; null is stored as an empty map
   */
  public AtCommandExecutorContext(AtSign atSign, AtKeys keys, Map<String, Object> config) {
    this.atSign = checkNotNull(atSign, "atSign not set");
    this.keys = keys;
    this.config = config != null ? unmodifiableMap(new LinkedHashMap<>(config)) : emptyMap();
    this.challenge = new AtomicReference<>();
  }

  /**
   * Retains the challenge returned by the initial {@code from:}, so the authentication that follows
   * on the same connection can reuse it.
   *
   * @param challenge the challenge from the server's {@code from:} response
   */
  public void setChallenge(String challenge) {
    this.challenge.set(challenge);
  }

  /**
   * Returns the retained {@code from:} challenge and clears it, so it is consumed at most once.
   * Whichever authentication (CRAM or PKAM) sends its digest first consumes it; anything else on the
   * same connection gets {@code null} and must issue its own {@code from:}.
   *
   * @return the retained challenge, or {@code null} if none is available
   */
  public String consumeChallenge() {
    return challenge.getAndSet(null);
  }
}
