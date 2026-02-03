package org.atsign.client.cli;

import org.atsign.client.util.KeysUtil;
import org.atsign.common.AtSign;

public class DumpKeys {
    public static void main(String[] args) throws Exception {
      System.out.println(KeysUtil.dump(KeysUtil.loadKeys(new AtSign(args[0]))));
    }
}
