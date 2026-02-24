package org.atsign.client.connection.netty;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.atsign.common.AtSign.createAtSign;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import org.atsign.client.connection.netty.NettyAtClientConnection.NettyAtClientConnectionBuilder;
import org.atsign.common.AtSign;
import org.atsign.cucumber.helpers.Helpers;
import org.atsign.virtualenv.VirtualEnv;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import lombok.extern.slf4j.Slf4j;

@Slf4j
class NettyAtClientConnectionIT {

  @BeforeAll
  public static void classSetup() {
    if (!Helpers.isHostPortReachable("vip.ve.atsign.zone:64", SECONDS.toMillis(2))) {
      VirtualEnv.setUp();
    }
  }

  @Test
  void testResolveAtServer() throws Exception {
    NettyAtEndpointSupplier provider = NettyAtEndpointSupplier.builder()
        .rootUrl("vip.ve.atsign.zone:64")
        .atsign(createAtSign("colin"))
        .build();
    assertThat(provider.get().matches("\\S+:\\d+"), is(true));
  }


  @Test
  void testScan() throws Exception {
    NettyAtEndpointSupplier provider = NettyAtEndpointSupplier.builder()
        .rootUrl("vip.ve.atsign.zone:64")
        .atsign(createAtSign("colin"))
        .build();
    try (NettyAtClientConnection connection = NettyAtClientConnection.builder().endpoint(provider).build()) {
      assertThat(connection.sendSync("scan"), Matchers.startsWith("data:"));
    }
  }

  @Test
  void testUnauthenticatedMonitorAttempt() throws Exception {
    AtSign atSign = createAtSign("colin");

    NettyAtEndpointSupplier provider = NettyAtEndpointSupplier.builder()
        .rootUrl("vip.ve.atsign.zone:64")
        .atsign(atSign)
        .build();

    NettyAtClientConnectionBuilder builder = NettyAtClientConnection.builder()
        .endpoint(provider);

    try (NettyAtClientConnection connection = builder.build()) {
      List<String> notifications = new CopyOnWriteArrayList<>();
      connection.sendSync("monitor", notifications::add);
      await().until(() -> !notifications.isEmpty());
      assertThat(notifications.get(0), Matchers.startsWith("error:"));
    }
  }
}
