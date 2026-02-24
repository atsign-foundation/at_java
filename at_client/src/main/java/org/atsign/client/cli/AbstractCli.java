package org.atsign.client.cli;

import static org.atsign.client.util.Preconditions.checkNotNull;

import java.io.File;
import java.util.concurrent.TimeUnit;

import org.atsign.client.api.AtKeys;
import org.atsign.client.connection.api.AtClientConnection;
import org.atsign.client.connection.common.SimpleReconnectStrategy;
import org.atsign.client.connection.netty.NettyAtClientConnection;
import org.atsign.client.connection.netty.NettyAtClientConnection.NettyAtClientConnectionBuilder;
import org.atsign.client.connection.netty.NettyAtEndpointSupplier;
import org.atsign.client.connection.protocol.Authentication;
import org.atsign.client.util.KeysUtil;
import org.atsign.common.AtException;
import org.atsign.common.AtSign;
import org.atsign.common.exceptions.AtClientConfigException;

import picocli.CommandLine.ITypeConverter;
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
  protected int connectionRetries = 1;
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
    return keysFile != null ? keysFile : KeysUtil.getKeysFile(atSign);
  }


  protected static String ensureNotNull(String value, String defaultValue) {
    return value != null ? value : defaultValue;
  }

  protected AtClientConnection createConnection(String rootUrl, AtSign atSign, int retries) throws AtException {
    return creatConnectionBuilder(rootUrl, atSign, retries, verbose).build();
  }

  protected AtClientConnection createAuthenticatedConnection(String rootUrl, AtSign atSign, int retries)
      throws AtException {
    return creatConnectionBuilder(rootUrl, atSign, retries, verbose)
        .onReady(Authentication.pkamAuthenticator(atSign, getKeys()))
        .build();
  }

  private static NettyAtClientConnectionBuilder creatConnectionBuilder(String rootUrl, AtSign atSign, int retries,
                                                                       boolean verbose) {
    NettyAtEndpointSupplier endpoint = NettyAtEndpointSupplier.builder()
        .rootUrl(checkNotNull(rootUrl, "root server endpoint not set"))
        .atsign(checkNotNull(atSign, "atsign not set"))
        .build();
    SimpleReconnectStrategy reconnect = SimpleReconnectStrategy.builder()
        .maxReconnectRetries(retries)
        .reconnectPauseMillis(TimeUnit.SECONDS.toMillis(2))
        .build();
    return NettyAtClientConnection.builder()
        .endpoint(endpoint)
        .reconnect(reconnect)
        .isVerbose(verbose);
  }

  protected AtKeys getKeys() {
    try {
      File file = checkExists(getAtKeysFile(keysFile, atSign));
      return KeysUtil.loadKeys(file);
    } catch (AtClientConfigException e) {
      throw new RuntimeException(e);
    }
  }

  static class AtSignConverter implements ITypeConverter<AtSign> {
    @Override
    public AtSign convert(String s) {
      return new AtSign(s);
    }
  }
}
