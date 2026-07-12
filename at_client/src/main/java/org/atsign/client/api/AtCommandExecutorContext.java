package org.atsign.client.api;

import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

/**
 * The authentication context an {@link AtCommandExecutor} carries for the life of a connection: the
 * identity it authenticates as ({@link #getAtSign() atSign}, {@link #getKeys() keys},
 * {@link #getConfig() config}) together with the single-use challenge from the {@code from:} the
 * executor issues as its first command once ready.
 *
 * <p>
 * Grouping these behind {@link AtCommandExecutor#getContext()} lets the authentication commands
 * take
 * exactly what they need from one accessor rather than the executor interface growing a separate
 * method per field.
 *
 * <p>
 * The identity fields are fixed for the life of the context. The challenge is session state: the
 * executor {@link #setChallenge(String) retains} it once the initial {@code from:} completes,
 * callers {@link #consumeChallenge() consume} it at most once (the server's {@code from:} challenge
 * is single-use), and the executor {@link #clearChallenge() clears} it on disconnect — a challenge
 * is only valid for the server session that issued it.
 */
public class AtCommandExecutorContext {

  /**
   * A context with no identity and no challenge, returned by executors that were not configured with
   * an atSign (and so issue no initial {@code from:}). {@link #consumeChallenge()} always yields
   * {@code null}, so authentication falls back to sending its own {@code from:}.
   */
  public static final AtCommandExecutorContext EMPTY = new AtCommandExecutorContext(null, null, null);

  private final AtSign atSign;

  private final AtKeys keys;

  private final Map<String, Object> config;

  private final AtomicReference<String> challenge = new AtomicReference<>();

  public AtCommandExecutorContext(AtSign atSign, AtKeys keys, Map<String, Object> config) {
    this.atSign = atSign;
    this.keys = keys;
    this.config = config;
  }

  /**
   * @return the atSign this executor authenticates as, or {@code null} if none was configured
   */
  public AtSign getAtSign() {
    return atSign;
  }

  /**
   * @return the keys this executor authenticates with, or {@code null} if none were configured (e.g.
   *         an onboarding executor whose keys are generated mid-flow and supplied to the command
   *         directly)
   */
  public AtKeys getKeys() {
    return keys;
  }

  /**
   * @return the config sent in the {@code from:} command, or {@code null} if none was configured
   */
  public Map<String, Object> getConfig() {
    return config;
  }

  /**
   * Retains the challenge returned by the initial {@code from:}. Called by the executor once that
   * command completes on the ready thread.
   *
   * @param challenge the challenge from the server's {@code from:} response
   */
  public void setChallenge(String challenge) {
    this.challenge.set(challenge);
  }

  /**
   * Returns the retained {@code from:} challenge and clears it, so it is consumed at most once.
   * Whichever authentication (CRAM or PKAM) sends its digest first consumes the challenge; a second
   * authentication on the same connection (e.g. onboarding, which does CRAM then PKAM) gets
   * {@code null} and must issue its own {@code from:}.
   *
   * @return the retained challenge, or {@code null} if none is available
   */
  public String consumeChallenge() {
    return challenge.getAndSet(null);
  }

  /**
   * Clears any retained challenge. Called by the executor on disconnect — a challenge is only valid
   * for the server session that issued it, so it must not survive into the next connection.
   */
  public void clearChallenge() {
    challenge.set(null);
  }
}
