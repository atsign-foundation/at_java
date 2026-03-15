package org.atsign.client.impl.netty;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.atsign.client.api.AtSign.createAtSign;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import org.atsign.client.api.AtSign;
import org.atsign.client.impl.netty.NettyAtCommandExecutor.NettyAtCommandExecutorBuilder;
import org.atsign.cucumber.helpers.Helpers;
import org.atsign.virtualenv.VirtualEnv;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import lombok.extern.slf4j.Slf4j;

@Slf4j
class NettyAtCommandExecutorIT {

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
    try (NettyAtCommandExecutor executor = NettyAtCommandExecutor.builder().endpoint(provider).build()) {
      assertThat(executor.sendSync("scan"), Matchers.startsWith("data:"));
    }
  }

  @Test
  void testUnauthenticatedMonitorAttempt() throws Exception {
    AtSign atSign = createAtSign("colin");

    NettyAtEndpointSupplier provider = NettyAtEndpointSupplier.builder()
        .rootUrl("vip.ve.atsign.zone:64")
        .atsign(atSign)
        .build();

    NettyAtCommandExecutorBuilder builder = NettyAtCommandExecutor.builder()
        .endpoint(provider);

    try (NettyAtCommandExecutor executor = builder.build()) {
      List<String> notifications = new CopyOnWriteArrayList<>();
      executor.sendSync("monitor", notifications::add);
      await().until(() -> !notifications.isEmpty());
      assertThat(notifications.get(0), Matchers.startsWith("error:"));
    }
  }
}
