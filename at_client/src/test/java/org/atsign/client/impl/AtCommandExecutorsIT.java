package org.atsign.client.impl;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.File;

import org.atsign.client.api.AtCommandExecutor;
import org.atsign.client.api.AtKeys;
import org.atsign.client.api.AtSign;
import org.atsign.client.impl.AtCommandExecutors.AtCommandExecutorBuilder;
import org.atsign.client.impl.commands.ScanCommands;
import org.atsign.client.impl.common.ReconnectStrategy;
import org.atsign.client.impl.util.KeysUtils;
import org.atsign.cucumber.helpers.Helpers;
import org.atsign.virtualenv.VirtualEnv;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import lombok.extern.slf4j.Slf4j;

@Slf4j
class AtCommandExecutorsIT {

  private static AtSign atSign;
  private static AtKeys keys;

  @BeforeAll
  public static void classSetup() throws Exception {
    if (!Helpers.isHostPortReachable("vip.ve.atsign.zone:64", SECONDS.toMillis(2))) {
      VirtualEnv.setUp();
    }
    atSign = AtSign.of("colin");
    keys = KeysUtils.loadKeys(new File("target/at_demo_data/lib/assets/atkeys/@colin.atKeys"));
  }

  @Test
  void testBuildWithRootHost() throws Exception {
    AtCommandExecutorBuilder builder = AtCommandExecutors.builder()
        .url("vip.ve.atsign.zone")
        .atSign(atSign)
        .keys(keys)
        .reconnect(ReconnectStrategy.NONE);
    try (AtCommandExecutor executor = builder.build()) {
      assertThat(ScanCommands.scan(executor, true, ".*"), is(not(empty())));
    }
  }

  @Test
  void testBuildWithRootHostButNoAuthentication() throws Exception {
    AtCommandExecutorBuilder builder = AtCommandExecutors.builder()
        .url("vip.ve.atsign.zone")
        .atSign(atSign)
        .reconnect(ReconnectStrategy.NONE);
    try (AtCommandExecutor executor = builder.build()) {
      assertThat(ScanCommands.scan(executor, true, ".*"), is(not(empty())));
    }
  }

  @Test
  void testBuildWithProxyUrl() throws Exception {
    AtCommandExecutorBuilder builder = AtCommandExecutors.builder()
        .url("proxy:vip.ve.atsign.zone:25026")
        .atSign(atSign)
        .keys(keys)
        .reconnect(ReconnectStrategy.NONE);
    try (AtCommandExecutor executor = builder.build()) {
      assertThat(ScanCommands.scan(executor, true, ".*"), is(not(empty())));
    }
  }

  @Test
  void testBuildWithProxyUrlEnforcesAuthentication() throws Exception {
    AtCommandExecutorBuilder builder = AtCommandExecutors.builder()
        .url("proxy:vip.ve.atsign.zone:25026")
        .reconnect(ReconnectStrategy.NONE);
    Exception ex = assertThrows(Exception.class, () -> builder.build());
    assertThat(ex.getMessage(), containsString("atSign not set"));
  }

}
