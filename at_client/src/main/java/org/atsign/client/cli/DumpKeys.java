package org.atsign.client.cli;

import lombok.extern.slf4j.Slf4j;
import org.atsign.client.api.AtKeys;
import org.atsign.client.util.KeysUtil;
import org.atsign.common.AtSign;

@Slf4j
public class DumpKeys {
  public static void main(String[] args) throws Exception {
    AtSign atSign = new AtSign(args[0]);
    AtKeys keys = KeysUtil.loadKeys(atSign);
    System.out.println(KeysUtil.dump(keys));
  }
}
