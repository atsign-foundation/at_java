package org.atsign.common;

import java.util.Map;

import org.atsign.client.api.AtClient;
import org.atsign.client.api.Secondary;
import org.atsign.client.util.ArgsUtil;
import org.atsign.common.ResponseTransformers.LlookupMetadataResponseTransformer;

public class ResponseTransformerTests {

    public static void main(String[] args) {
        String ATSIGN_STR = "@sportsunconscious";
        String HOST = "root.atsign.wtf";
        int PORT = 64;

        String KEY_NAME = "@farinataanxious:shared_key@sportsunconscious";


        AtClient atClient = null;
        try {
            AtSign atSign = new AtSign(ATSIGN_STR);
            Secondary.AddressFinder addressFinder = ArgsUtil.createAddressFinder(HOST + ":" + PORT);
            Secondary.Address address = addressFinder.findSecondary(atSign);
            atClient = AtClient.withRemoteSecondary(address, atSign);
            LlookupMetadataResponseTransformer transformer = new LlookupMetadataResponseTransformer();
            Map<String, Object> map = transformer.transform(atClient.executeCommand("llookup:meta:" + KEY_NAME, true));

            for (Map.Entry<String, Object> entry : map.entrySet()) {
                System.out.print(entry.getKey() + ": ");
                System.out.print((entry.getValue() != null ? "(" + entry.getValue().getClass().toString() + ")" : "null") + "\n");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}