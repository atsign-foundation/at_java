package org.atsign.client.impl;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

import java.io.File;

import org.atsign.client.api.AtClient;
import org.atsign.client.api.AtKeys;
import org.atsign.client.api.AtSign;
import org.atsign.client.impl.common.ReconnectStrategy;
import org.atsign.client.impl.util.KeysUtils;
import org.atsign.cucumber.helpers.Helpers;
import org.atsign.virtualenv.VirtualEnv;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class AtClientsIT {

  private static AtSign atSign;
  private static AtKeys keys;
  private static File keysFile;
  public static String AT_SIGN_KEYS_DIR;
  public static String ATSIGN_KEYS_SUFFIX;

  @BeforeAll
  public static void classSetup() throws Exception {
    if (!Helpers.isHostPortReachable("vip.ve.atsign.zone:64", SECONDS.toMillis(2))) {
      VirtualEnv.setUp();
    }
    atSign = AtSign.createAtSign("colin");
    keysFile = new File("target/at_demo_data/lib/assets/atkeys/@colin.atKeys");
    keys = KeysUtils.loadKeys(keysFile);
    AT_SIGN_KEYS_DIR = KeysUtils.expectedKeysFilesLocation;
    ATSIGN_KEYS_SUFFIX = KeysUtils.keysFileSuffix;
    KeysUtils.expectedKeysFilesLocation = "target/at_demo_data/lib/assets/atkeys";
    KeysUtils.keysFileSuffix = ".atKeys";
  }

  @AfterAll
  public static void classTeardown() {
    KeysUtils.expectedKeysFilesLocation = AT_SIGN_KEYS_DIR;
    KeysUtils.keysFileSuffix = ATSIGN_KEYS_SUFFIX;
  }

  @Test
  void testBuildWithoutKeys() throws Exception {
    AtClients.AtClientBuilder builder = AtClients.builder()
        .url("vip.ve.atsign.zone")
        .atSign(atSign)
        .reconnect(ReconnectStrategy.NONE);
    try (AtClient client = builder.build()) {
      assertThat(client.getAtKeys(".*", false).get(), is(not(empty())));
    }
  }

  @Test
  void testBuildWithKeys() throws Exception {
    AtClients.AtClientBuilder builder = AtClients.builder()
        .url("vip.ve.atsign.zone")
        .atSign(atSign)
        .keys(keys)
        .reconnect(ReconnectStrategy.NONE);
    try (AtClient client = builder.build()) {
      assertThat(client.getAtKeys(".*", false).get(), is(not(empty())));
    }
  }

  @Test
  void testBuildWithKeysPathSetToDirectory() throws Exception {
    AtClients.AtClientBuilder builder = AtClients.builder()
        .url("vip.ve.atsign.zone")
        .atSign(atSign)
        .keysPath(keysFile.getParentFile().getAbsolutePath())
        .reconnect(ReconnectStrategy.NONE);
    try (AtClient client = builder.build()) {
      assertThat(client.getAtKeys(".*", false).get(), is(not(empty())));
    }
  }

  @Test
  void testBuildWithKeysPathSetToFile() throws Exception {
    AtClients.AtClientBuilder builder = AtClients.builder()
        .url("vip.ve.atsign.zone")
        .atSign(atSign)
        .keysPath(keysFile.getAbsolutePath())
        .reconnect(ReconnectStrategy.NONE);
    try (AtClient client = builder.build()) {
      assertThat(client.getAtKeys(".*", false).get(), is(not(empty())));
    }
  }

}
