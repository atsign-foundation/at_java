package org.atsign.client.cli;

import org.atsign.client.util.KeysUtil;
import org.atsign.common.AtSign;

import java.util.Map;

public class DumpKeys {
    public static void main(String[] args) throws Exception {
        Map<String, String> keys = KeysUtil.loadKeys(new AtSign(args[0]));
        for (String key : keys.keySet()) {
            System.out.println("\tkey: " + key + "\n\t\tvalue: " + keys.get(key) + "\n");
        }
    }
}
