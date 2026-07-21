package org.atsign.client.impl.cli;

import java.io.File;
import java.util.concurrent.TimeUnit;

import org.atsign.client.api.AtCommandExecutor;
import org.atsign.client.api.AtCommandExecutorContext;
import org.atsign.client.api.AtKeys;
import org.atsign.client.api.AtSign;
import org.atsign.client.impl.AtCommandExecutors;
import org.atsign.client.impl.AtCommandExecutors.AtCommandExecutorBuilder;
import org.atsign.client.impl.commands.AuthenticationCommands;
import org.atsign.client.impl.common.SimpleReconnectStrategy;
import org.atsign.client.impl.exceptions.AtClientConfigException;
import org.atsign.client.impl.exceptions.AtException;
import org.atsign.client.impl.util.KeysUtils;

import picocli.CommandLine.Option;

/**
 * Base class for Command Line Interface utilities. Holds common fields such as root server
 * the {@link AtSign} which is connecting and the file which contains the {@link AtKeys}
 *
 * @param <T> used to provide fluent builder style API
 */
public abstract class AbstractCli<T extends AbstractCli<T>> {

  protected String rootUrl = "root.atsign.org";
  protected AtSign atSign;
  protected File keysFile;
  protected int connectionRetries = 2;
  private boolean verbose = false;

  protected abstract T self();

  public T setVerbose(boolean isVerbose) {
    this.verbose = isVerbose;
    return self();
  }

  public T setVerbose() {
    return setVerbose(true);
  }

  @Option(names = {"-r", "--root"}, paramLabel = "HOST:PORT",
      description = "atDirectory (aka root) server domain (e.g., root.atsign.org)")
  public T setRootUrl(String rootUrl) {
    this.rootUrl = rootUrl;
    return self();
  }

  @Option(names = {"-a", "--atsign"}, description = "the atsign e.g. @colin", paramLabel = "ATSIGN",
      converter = AtSignConverter.class)
  public T setAtSign(AtSign atSign) {
    this.atSign = atSign;
    return self();
  }

  @Option(names = {"-k", "--keys"}, paramLabel = "PATH", description = "path to atKeys file to use / create")
  public T setKeysFile(String path) {
    this.keysFile = new File(path);
    return self();
  }

  protected static File checkNotExists(File f) {
    if (f.exists()) {
      throw new IllegalArgumentException(f.getPath() + " would be overwritten");
    }
    return f;
  }

  protected static File checkExists(File f) {
    if (!f.exists()) {
      throw new IllegalArgumentException(f.getPath() + " not found");
    }
    return f;
  }

  protected static File getAtKeysFile(File keysFile, AtSign atSign) {
    return keysFile != null ? keysFile : KeysUtils.getKeysFile(atSign);
  }


  protected static String ensureNotNull(String value, String defaultValue) {
    return value != null ? value : defaultValue;
  }

  protected AtCommandExecutor createConnection(String rootUrl, AtSign atSign, int retries) throws AtException {
    // no keys: the builder wires an onReady that issues from:@atSign only (so proxies can route it)
    return connectionBuilder(rootUrl, retries, verbose).atSign(atSign).build();
  }

  protected AtCommandExecutor createAuthenticatedConnection(String rootUrl, AtSign atSign, int retries)
      throws AtException {
    // keys present: the builder wires from:@atSign followed by PKAM (reusing the from: challenge)
    return connectionBuilder(rootUrl, retries, verbose).atSign(atSign).keys(getKeys()).build();
  }

  /**
   * A connection that issues from:@atSign on connect (so proxies / gateways can route it), wiring the
   * {@code from:} over the given {@code context} so the challenge it retains is the one an imperative
   * flow driving the connection (e.g. onboarding: scan, then CRAM, then PKAM) later consumes. No
   * on-ready authentication is wired — the caller authenticates imperatively.
   */
  protected AtCommandExecutor createConnectionSendingFrom(AtCommandExecutorContext context, int retries)
      throws AtException {
    return connectionBuilder(rootUrl, retries, verbose)
        .atSign(context.getAtSign())
        .onReady(AuthenticationCommands.sendFrom(context))
        .build();
  }

  /**
   * A connection that does NOT issue {@code from:} on connect — its initial {@code from:} is issued
   * by the caller's own first command — for flows that authenticate imperatively and must control the
   * timing (e.g. a pending-retry loop), where a connect-time {@code from:} challenge could go stale
   * before it is used. No onReady {@code from:} is wired, so the caller's first command (its
   * authentication) establishes the atSign for proxies / gateways.
   */
  protected AtCommandExecutor createConnectionDeferringFrom(String rootUrl, AtSign atSign, int retries)
      throws AtException {
    return connectionBuilder(rootUrl, retries, verbose).atSign(atSign).onReady(executor -> {
    }).build();
  }

  /**
   * Creates an {@link AtCommandExecutorContext} carrying this CLI's atSign and the given {@code keys}
   * (the identity being onboarded), to be passed to
   * {@link #createConnectionSendingFrom(AtCommandExecutorContext, int)} and threaded into the
   * imperative onboarding flow, which reuses the connection's {@code from:} challenge.
   */
  protected AtCommandExecutorContext newConnectionContext(AtKeys keys) {
    return new AtCommandExecutorContext(atSign, keys, AtCommandExecutors.createClientConfig(null));
  }

  private static AtCommandExecutorBuilder connectionBuilder(String rootUrl, int retries, boolean verbose) {
    SimpleReconnectStrategy reconnect = SimpleReconnectStrategy.builder()
        .maxReconnectRetries(retries)
        .reconnectPauseMillis(TimeUnit.SECONDS.toMillis(2))
        .build();
    return AtCommandExecutors.builder()
        .url(rootUrl)
        .reconnect(reconnect)
        .isVerbose(verbose);
  }

  protected AtKeys getKeys() {
    try {
      File file = checkExists(getAtKeysFile(keysFile, atSign));
      return KeysUtils.loadKeys(file);
    } catch (AtClientConfigException e) {
      throw new RuntimeException(e);
    }
  }

}
