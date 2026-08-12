package org.atsign.client.impl.cli;

import static org.atsign.client.api.AtSign.createAtSign;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.net.ServerSocket;

import org.atsign.client.api.AtCommandExecutor;
import org.atsign.client.api.AtCommandExecutorContext;
import org.atsign.client.impl.exceptions.AtException;
import org.junit.jupiter.api.Test;

class AbstractCliTest {

  private static class TestCli extends AbstractCli<TestCli> {
    @Override
    protected TestCli self() {
      return this;
    }

    AtCommandExecutor connectSendingFrom(AtCommandExecutorContext context) throws AtException {
      return createConnectionSendingFrom(context);
    }
  }

  /**
   * A proxy url so that the connection is attempted directly rather than resolved through the root
   * server, pointing at a port nothing is listening on so that it fails promptly and locally.
   */
  private String unservedProxyUrl() throws IOException {
    try (ServerSocket socket = new ServerSocket(0)) {
      return "proxy:localhost:" + socket.getLocalPort();
    }
  }

  @Test
  void testCreateConnectionSendingFromPassesOnlyTheSharedContext() throws Exception {
    TestCli cli = new TestCli().setAtSign(createAtSign("@alice"));
    cli.setRootUrl(unservedProxyUrl());
    AtCommandExecutorContext context = cli.newConnectionContext(null);

    // the executor builder rejects a context combined with a loose atSign / keys / config, so an
    // AtException (rather than an IllegalArgumentException) is what says the onboarding flow's
    // context reaches the connection as the one identity it is built from
    assertThrows(AtException.class, () -> cli.connectSendingFrom(context));
  }
}
