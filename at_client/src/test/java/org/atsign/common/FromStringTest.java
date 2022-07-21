package org.atsign.common;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.text.ParseException;

import org.atsign.client.api.AtClient;
import org.atsign.client.api.Secondary;
import org.atsign.client.util.ArgsUtil;
import org.atsign.common.Keys.AtKey;

public class FromStringTest {

    public static void main(String[] args) {
        String KEY_NAME_STR = "public:publickey@sportsunconscious";
        String AT_SIGN_STR = "@sportsunconscious";
        String HOST = "root.atsign.wtf";
        int PORT = 64;

        AtClient atClient = null;
        try {
            AtSign atSign = new AtSign(AT_SIGN_STR);
            Secondary.AddressFinder addressFinder = ArgsUtil.createAddressFinder(HOST + ":" + PORT);
            Secondary.Address address = addressFinder.findSecondary(atSign);
            atClient = AtClient.withRemoteSecondary(address, atSign);
        } catch (AtException | IOException e) {
            e.printStackTrace();
        }
        
        AtKey atKey = null;
        try {
            Secondary.Response response = atClient.executeCommand("llookup:meta:" + KEY_NAME_STR, true);
            atKey = Keys.fromString(KEY_NAME_STR, response);
        } catch (AtException | ParseException e) {
            System.err.println(e);
            e.printStackTrace();
        }

        System.out.println(atKey.name);
    }
    
}