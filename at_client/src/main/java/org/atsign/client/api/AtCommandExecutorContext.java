package org.atsign.client.api;

import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import lombok.Value;

/**
 * The identity a connection authenticates as — its {@link #getAtSign() atSign},
 * {@link #getKeys() keys} and {@link #getConfig() config} — together with the single-use challenge
 * from the {@code from:} that is issued as the first command once the connection is ready.
 *
 * <p>
 * The context is created by the builder (see
 * {@code AtCommandExecutors#createCommandExecutor}) and closed over by the {@code onReady}
 * consumers
 * it wires, so the {@code from:} sender can retain the challenge and the authentication that
 * follows
 * on the same connection can reuse it rather than issuing a second {@code from:}. The command
 * executor itself is pure transport and knows nothing about this context.
 *
 * <p>
 * The identity fields are fixed for the life of the context. The challenge is per-connection state:
 * {@link #setChallenge(String) retained} when the initial {@code from:} completes and
 * {@link #consumeChallenge() consumed} at most once (the server's {@code from:} challenge is
 * single-use). On reconnect the ready sequence re-runs, so a fresh challenge overwrites any
 * previous
 * one before it is consumed.
 */
@Value
public class AtCommandExecutorContext {

  AtSign atSign;

  AtKeys keys;

  Map<String, Object> config;

  @Getter(AccessLevel.NONE)
  @EqualsAndHashCode.Exclude
  @ToString.Exclude
  AtomicReference<String> challenge = new AtomicReference<>();

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
