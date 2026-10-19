package org.atsign.client.impl.netty;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;

import org.junit.jupiter.api.Test;

class TestServerTest {

  @Test
  void testCloseServerSocketReturnsOnceTheWorkerHasLeftAcceptSoThePortRebindsAtOnce() throws Exception {
    try (TestServer server = new TestServer()) {
      int port = server.getPort();
      await().pollInterval(1, MILLISECONDS).atMost(1, SECONDS).until(server::isAccepting);

      server.closeServerSocket();

      assertThat("worker is still inside accept()", server.isAccepting(), is(false));
      server.newServerSocket();
      assertThat(server.getPort(), equalTo(port));
    }
  }
}
