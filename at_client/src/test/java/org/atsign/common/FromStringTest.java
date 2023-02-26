package org.atsign.common;

import static org.junit.Assert.*;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.atsign.client.api.Secondary;
import org.atsign.common.Keys.AtKey;
import org.junit.Test;

public class FromStringTest {


    @Test
    public void fromStringTest0() throws AtException {
        // no llookup meta
        AtKey atKey = Keys.fromString("public:publickey@alice");

        assertEquals(true, atKey.metadata.isPublic);
        assertEquals(false, atKey.metadata.isEncrypted);
        assertEquals(false, atKey.metadata.isHidden);
        assertEquals("publickey", atKey.name);
        assertEquals("@alice", atKey.sharedBy.toString());
    }

    @Test
    public void fromStringTest1() throws AtException, JsonProcessingException {
        String KEY_NAME_STR = "public:publickey@bob";

        Secondary.Response response = new Secondary.Response();
        response.setRawDataResponse("{\"createdBy\":null,\"updatedBy\":null," +
                "\"createdAt\":\"2022-07-13 21:54:28.519Z\",\"updatedAt\":\"2022-07-13 21:54:28.519Z\"," +
                "\"availableAt\":\"2022-07-13 21:54:28.519Z\",\"expiresAt\":null," +
                "\"refreshAt\":null,\"status\":\"active\",\"version\":0," +
                "\"ttl\":0,\"ttb\":0,\"ttr\":null,\"ccd\":null," +
                "\"isBinary\":false,\"isEncrypted\":false," +
                "\"dataSignature\":null,\"sharedKeyEnc\":null,\"pubKeyCS\":null}");
        
        AtKey atKey = Keys.fromString(KEY_NAME_STR, response);

        assertEquals(KEY_NAME_STR, atKey.toString());
        assertEquals(true, atKey.metadata.isPublic);
        assertEquals("publickey", atKey.name);
        assertEquals("@bob", atKey.sharedBy.toString());
        assertEquals("2022-07-13T21:54:28.519Z", atKey.metadata.createdAt.toString());
        assertEquals("2022-07-13T21:54:28.519Z", atKey.metadata.updatedAt.toString());
        assertEquals("2022-07-13T21:54:28.519Z", atKey.metadata.availableAt.toString());
        assertNull(atKey.metadata.expiresAt);
        assertNull(atKey.metadata.refreshAt);
        assertEquals(0, atKey.metadata.ttl.intValue());
        assertEquals(0, atKey.metadata.ttb.intValue());
        assertNull(atKey.metadata.ttr);
        assertNull(atKey.metadata.ccd);
        assertEquals(false, atKey.metadata.isBinary);
        assertEquals(false, atKey.metadata.isEncrypted);
        assertNull(atKey.metadata.dataSignature);
        assertNull(atKey.metadata.sharedKeyEnc);
        assertNull(atKey.metadata.pubKeyCS);
    }

    @Test
    public void fromStringTest2() throws AtException, JsonProcessingException {
        String KEY_NAME_STR = "test@bob";

        @SuppressWarnings("SpellCheckingInspection") String LLOOKUP_META_STR = "{\"createdBy\":null,\"updatedBy\":null,\"createdAt\":\"2022-07-27 22:12:58.077Z\"," +
                "\"updatedAt\":\"2022-07-27 22:12:58.077Z\",\"availableAt\":\"2022-07-27 22:12:58.077Z\"," +
                "\"expiresAt\":\"2022-07-27 22:42:58.077Z\",\"refreshAt\":null,\"status\":\"active\",\"version\":0," +
                "\"ttl\":1800000,\"ttb\":0,\"ttr\":null,\"ccd\":null," +
                "\"isBinary\":false,\"isEncrypted\":true," +
                "\"dataSignature\":\"oIq0kHvwQieVrhOs4dJLN61qNP73bNLLNPTRW7tAdapIZF3kSMrNVCcTAWWWyzb2Tyii51uZ7zlIYmHWuS4tIE0lMzrUeXGcfQhOrdjkrxf4qEceNR1qLa7tDjOAb8xuhf/zJ3yaen8NGswfKWwQluga/52SchFClrR99xEI93s=\"," +
                "\"sharedKeyEnc\":null,\"pubKeyCS\":null}";

        Secondary.Response response = new Secondary.Response();
        response.setRawDataResponse(LLOOKUP_META_STR);
        
        AtKey atKey = Keys.fromString(KEY_NAME_STR, response);

        assertEquals(KEY_NAME_STR, atKey.toString());
        assertEquals(false, atKey.metadata.isPublic);
        assertEquals("test", atKey.name);
        assertEquals("@bob", atKey.sharedBy.toString());
        assertEquals("2022-07-27T22:12:58.077Z", atKey.metadata.createdAt.toString());
        assertEquals("2022-07-27T22:12:58.077Z", atKey.metadata.updatedAt.toString());
        assertEquals("2022-07-27T22:12:58.077Z", atKey.metadata.availableAt.toString());
        assertEquals("2022-07-27T22:42:58.077Z", atKey.metadata.expiresAt.toString());
        assertNull(atKey.metadata.refreshAt);
        assertEquals(1800000, atKey.metadata.ttl.intValue());
        assertEquals(0, atKey.metadata.ttb.intValue());
        assertNull(atKey.metadata.ttr);
        assertNull(atKey.metadata.ccd);
        assertFalse(atKey.metadata.isBinary);
        assertTrue(atKey.metadata.isEncrypted);
        //noinspection SpellCheckingInspection
        assertEquals("oIq0kHvwQieVrhOs4dJLN61qNP73bNLLNPTRW7tAdapIZF3kSMrNVCcTAWWWyzb2Tyii51uZ7zlIYmHWuS4tIE0lMzrUeXGcfQhOrdjkrxf4qEceNR1qLa7tDjOAb8xuhf/zJ3yaen8NGswfKWwQluga/52SchFClrR99xEI93s=", atKey.metadata.dataSignature);
        assertNull(atKey.metadata.sharedKeyEnc);
        assertNull(atKey.metadata.pubKeyCS);
    }

}
