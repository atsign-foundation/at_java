package org.atsign.client.impl;

import static org.atsign.client.api.AtSign.createAtSign;
import static org.atsign.client.impl.util.EncryptionUtils.generateRSAKeyPair;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.net.ServerSocket;
import java.util.Collections;

import org.atsign.client.api.AtCommandExecutorContext;
import org.atsign.client.api.AtKeys;
import org.atsign.client.impl.common.ReconnectStrategy;
import org.atsign.client.impl.exceptions.AtException;
import org.junit.jupiter.api.Test;

class AtCommandExecutorsTest {

  private static final String CONTEXT_WITH_LOOSE_ARGS =
      "both context and one or more of atSign,keys and config set";
  private static final String ATSIGN_NOT_SET = "atSign not set";

  private AtCommandExecutorContext aliceContext() {
    return AtClients.createContext(createAtSign("@alice"), null, null);
  }

  private AtKeys keys() throws Exception {
    return AtKeys.builder().apkamKeyPair(generateRSAKeyPair()).build();
  }

  /**
   * A proxy url so that the builder connects directly rather than resolving through the root server,
   * pointing at a port nothing is listening on so that a build which gets as far as connecting fails
   * promptly and locally.
   */
  private String unservedProxyUrl() throws IOException {
    try (ServerSocket socket = new ServerSocket(0)) {
      return "proxy:localhost:" + socket.getLocalPort();
    }
  }

  @Test
  void testContextIsRejectedAlongsideAnAtSign() throws Exception {
    String url = unservedProxyUrl();
    AtCommandExecutorContext context = aliceContext();

    // the context is the connection's whole identity: a second, disagreeing atSign would connect
    // to bob's atServer and then authenticate as alice
    IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                                               () -> AtCommandExecutors.builder().url(url).context(context)
                                                   .atSign(createAtSign("@bob")).build());

    assertEquals(CONTEXT_WITH_LOOSE_ARGS, ex.getMessage());
  }

  @Test
  void testContextIsRejectedAlongsideKeys() throws Exception {
    String url = unservedProxyUrl();
    AtCommandExecutorContext context = aliceContext();
    AtKeys keys = keys();

    // the context carries the keys the onReady sequence authenticates with, so loose keys would
    // be silently discarded rather than used
    IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                                               () -> AtCommandExecutors.builder().url(url).context(context).keys(keys)
                                                   .build());

    assertEquals(CONTEXT_WITH_LOOSE_ARGS, ex.getMessage());
  }

  @Test
  void testContextIsRejectedAlongsideConfig() throws Exception {
    String url = unservedProxyUrl();
    AtCommandExecutorContext context = aliceContext();

    // likewise the context's config is the one the from: carries, so a loose config would be lost
    IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                                               () -> AtCommandExecutors.builder()
                                                   .url(url)
                                                   .context(context)
                                                   .config(Collections.singletonMap("myOwnKey", "myOwnValue"))
                                                   .build());

    assertEquals(CONTEXT_WITH_LOOSE_ARGS, ex.getMessage());
  }

  @Test
  void testAnAtSignIsRequiredWhenNoContextIsSupplied() throws Exception {
    String url = unservedProxyUrl();

    IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                                               () -> AtCommandExecutors.builder().url(url).build());

    assertEquals(ATSIGN_NOT_SET, ex.getMessage());
  }

  @Test
  void testKeysAreRejectedWithoutAnAtSign() throws Exception {
    String url = unservedProxyUrl();
    AtKeys keys = keys();

    // there is no anonymous connection to fall back to, so keys alone are not a configuration
    IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                                               () -> AtCommandExecutors.builder().url(url).keys(keys).build());

    assertEquals(ATSIGN_NOT_SET, ex.getMessage());
  }

  @Test
  void testAContextAloneIsASufficientConfiguration() throws Exception {
    String url = unservedProxyUrl();
    AtCommandExecutorContext context = aliceContext();

    // an AtException (rather than an IllegalArgumentException) means the identity and the endpoint
    // were both satisfied from the context alone, and only the connection failed
    assertThrows(AtException.class, () -> AtCommandExecutors.builder()
        .url(url)
        .context(context)
        .reconnect(ReconnectStrategy.NONE)
        .awaitReadyMillis(500L)
        .build());
  }
}
