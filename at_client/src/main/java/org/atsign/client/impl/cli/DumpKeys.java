package org.atsign.client.impl.cli;

import lombok.extern.slf4j.Slf4j;
import org.atsign.client.api.AtKeys;
import org.atsign.client.impl.util.KeysUtils;
import org.atsign.client.api.AtSign;

import static org.atsign.client.api.AtSign.createAtSign;

/**
 * Utility which, given an {@link AtSign} will load {@link AtKeys} from the
 * default location and dump the contents to stdout
 */
@Slf4j
public class DumpKeys {
  public static void main(String[] args) throws Exception {
    AtSign atSign = createAtSign(args[0]);
    AtKeys keys = KeysUtils.loadKeys(atSign);
    System.out.println(KeysUtils.dump(keys));
  }
}
