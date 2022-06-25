package org.atsign.common;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

import org.atsign.client.util.StringUtil;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class StringUtilTest {
    @Before
    public void setUp() {

    }

    @Test
    public void testGetRawStringArrayFromScanRawResponseString() {
        String rawResponseStr = "[\"@farinataanxious:lemon@sportsunconscious\",\"@farinataanxious:shared_key@sportsunconscious\",\"@farinataanxious:test@sportsunconscious\",\"@sportsunconscious:shared_key@sportsunconscious\",\"@sportsunconscious:signing_privatekey@sportsunconscious\",\"cached:public:publickey@farinataanxious\",\"public:publickey@sportsunconscious\",\"public:signing_publickey@sportsunconscious\",\"shared_key.farinataanxious@sportsunconscious\",\"shared_key.sportsunconscious@sportsunconscious\"]";
        String[] expected = {
            "@farinataanxious:lemon@sportsunconscious",
            "@farinataanxious:shared_key@sportsunconscious",
            "@farinataanxious:test@sportsunconscious",
            "@sportsunconscious:shared_key@sportsunconscious",
            "@sportsunconscious:signing_privatekey@sportsunconscious",
            "cached:public:publickey@farinataanxious",
            "public:publickey@sportsunconscious",
            "public:signing_publickey@sportsunconscious",
            "shared_key.farinataanxious@sportsunconscious",
            "shared_key.sportsunconscious@sportsunconscious",
        };
        String[] actual = StringUtil.getRawStringArrayFromScanRawResponseString(rawResponseStr);

        assertArrayEquals("rawResponse to String[]", expected, actual);
    }

    @Test
    public void testGetRawStringArrayFromScanRawResponseString2() {
        String rawResponseStr = "[\"public:signing_publickey@sportsunconscious\",\"shared_key.farinataanxious@sportsunconscious\",\"shared_key.sportsunconscious@sportsunconscious\"]";
        String[] expected = {
            "public:signing_publickey@sportsunconscious",
            "shared_key.farinataanxious@sportsunconscious",
            "shared_key.sportsunconscious@sportsunconscious"
        };
        String[] actual = StringUtil.getRawStringArrayFromScanRawResponseString(rawResponseStr);

        assertArrayEquals("rawResponse to String[]", expected, actual);
    }

    @After
    public void tearDown() {

    }
}
