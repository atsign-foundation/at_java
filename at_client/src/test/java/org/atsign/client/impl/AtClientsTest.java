package org.atsign.client.impl;

import static org.atsign.client.api.AtSign.createAtSign;
import static org.atsign.client.impl.util.EncryptionUtils.generateRSAKeyPair;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.matches;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.io.IOException;
import java.net.ServerSocket;
import java.util.Collections;
import java.util.Map;

import org.atsign.client.api.AtCommandExecutor;
import org.atsign.client.api.AtCommandExecutorContext;
import org.atsign.client.api.AtKeys;
import org.atsign.client.impl.commands.MonitorOptions;
import org.atsign.client.impl.commands.TestExecutorBuilder;
import org.atsign.client.impl.common.ReconnectStrategy;
import org.atsign.client.impl.exceptions.AtException;
import org.junit.jupiter.api.Test;

class AtClientsTest {

  private static final String CLIENT_CONFIG_WITH_CLIENT_ID = "from:@alice:clientConfig:.*\"clientId\".*";

  private AtKeys keys() throws Exception {
    return AtKeys.builder().apkamKeyPair(generateRSAKeyPair()).build();
  }

  @Test
  void testCreateContextEnrichesTheConfigWithTheClientIdentity() {
    AtCommandExecutorContext context = AtClients.createContext(createAtSign("@alice"), null, null);

    // clientId plus the client-config.properties entries, even though the caller passed no
    // config at all — this is what identifies the client to the At Server on the from:
    assertThat(context.getConfig(), hasKey("clientId"));
    assertThat(context.getConfig(), hasEntry("platform", "Java"));
    assertThat(context.getConfig(), hasKey("version"));
  }

  @Test
  void testCreateContextKeepsCallerSuppliedConfigEntries() {
    Map<String, Object> config = Collections.singletonMap("myOwnKey", "myOwnValue");

    AtCommandExecutorContext context = AtClients.createContext(createAtSign("@alice"), null, config);

    assertThat(context.getConfig(), hasKey("clientId"));
    assertThat(context.getConfig(), hasKey("myOwnKey"));
  }

  @Test
  void testMonitoringOnReadyIdentifiesTheClientInItsFromCommand() throws Exception {
    // the stub only answers a from: that carries a clientConfig, so a bare from: fails the run
    AtCommandExecutor executor = TestExecutorBuilder.builder()
        .stub("from:@alice:clientConfig:.+", "data:challenge")
        .stub("pkam:[^{].+", "data:success")
        .build();
    AtCommandExecutorContext context = AtClients.createContext(createAtSign("@alice"), keys(), null);

    AtClients.createMonitoringOnReady(context, MonitorOptions.builder().build(), s -> {
    }).accept(executor);

    verify(executor, times(1)).sendSync(matches(CLIENT_CONFIG_WITH_CLIENT_ID));
    verify(executor, times(1)).sendSync(matches("pkam:.*"));
  }

  @Test
  void testMonitoringOnReadyReusesAnAlreadyRetainedChallenge() throws Exception {
    AtCommandExecutor executor = TestExecutorBuilder.builder()
        .stub("pkam:[^{].+", "data:success")
        .build();
    AtCommandExecutorContext context = AtClients.createContext(createAtSign("@alice"), keys(), null);
    context.setChallenge("challenge");

    AtClients.createMonitoringOnReady(context, MonitorOptions.builder().build(), s -> {
    }).accept(executor);

    // no from: at all — the challenge the connection already holds is used instead, which is only
    // possible because the monitoring path and the executor share one context
    verify(executor, times(0)).sendSync(matches("from:.*"));
    verify(executor, times(1)).sendSync(matches("pkam:.*"));
    assertThat(context.consumeChallenge(), nullValue());
  }

  /**
   * A proxy url so that the builder connects directly rather than resolving through the root server,
   * pointing at a port nothing is listening on so that the build fails promptly and locally once it
   * gets as far as connecting.
   */
  private String unservedProxyUrl() throws IOException {
    try (ServerSocket socket = new ServerSocket(0)) {
      return "proxy:localhost:" + socket.getLocalPort();
    }
  }

  @Test
  void testCreateAtClientHandsTheExecutorBuilderTheSharedContextAndNothingElse() throws Exception {
    String url = unservedProxyUrl();
    AtKeys keys = keys();

    // the executor builder rejects a context combined with a loose atSign / keys / config, so
    // an AtException (rather than an IllegalArgumentException) is what says this passes the
    // connection's identity once, as the shared context
    assertThrows(AtException.class, () -> AtClients.builder()
        .atSign(createAtSign("@alice"))
        .keys(keys)
        .url(url)
        .reconnect(ReconnectStrategy.NONE)
        .awaitReadyMillis(500L)
        .build());
  }
}
