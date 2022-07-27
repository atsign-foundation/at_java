package org.atsign.common;

import static org.junit.Assert.assertEquals;

import java.util.Map;

import org.atsign.client.api.AtClient;
import org.atsign.client.api.Secondary;
import org.atsign.client.util.ArgsUtil;
import org.atsign.common.ResponseTransformers.LlookupMetadataResponseTransformer;
import org.junit.Test;

public class ResponseTransformerTest {

    @Test
    public void LlookupMetadataResponseTransformerTest() {
        String LLOOKUP_META_STR = "{\"createdBy\":null,\"updatedBy\":null,\"createdAt\":\"2022-07-13 21:54:28.519Z\",\"updatedAt\":\"2022-07-13 21:54:28.519Z\",\"availableAt\":\"2022-07-13 21:54:28.519Z\",\"expiresAt\":null,\"refreshAt\":null,\"status\":\"active\",\"version\":0,\"ttl\":0,\"ttb\":0,\"ttr\":null,\"ccd\":null,\"isBinary\":false,\"isEncrypted\":false,\"dataSignature\":null,\"sharedKeyEnc\":null,\"pubKeyCS\":null}";

        Secondary.Response response = new Secondary.Response();
        response.data = LLOOKUP_META_STR;

        LlookupMetadataResponseTransformer transformer = new LlookupMetadataResponseTransformer();
        Map<String, Object> map = transformer.transform(response);

        assertEquals(null, map.get("createdBy"));
        assertEquals(null, map.get("updatedBy"));
        assertEquals("2022-07-13 21:54:28.519Z", map.get("createdAt"));
        assertEquals("2022-07-13 21:54:28.519Z", map.get("updatedAt"));
        assertEquals("2022-07-13 21:54:28.519Z", map.get("availableAt"));
        assertEquals(null, map.get("expiresAt"));
        assertEquals(null, map.get("refreshAt"));
        assertEquals("active", map.get("status"));
        assertEquals(0, map.get("version"));
        assertEquals(0, map.get("ttl"));
        assertEquals(0, map.get("ttb"));
        assertEquals(null, map.get("ttr"));
        assertEquals(null, map.get("ccd"));
        assertEquals(false, map.get("isBinary"));
        assertEquals(false, map.get("isEncrypted"));
        assertEquals(null, map.get("dataSignature"));
        assertEquals(null, map.get("sharedKeyEnc"));
        assertEquals(null, map.get("pubKeyCS"));
    }

    @Test
    public void LlookupMetadataResponseTransformerTest2() {
        String LLOOKUP_META_STR = "{\"createdBy\":null,\"updatedBy\":null,\"createdAt\":\"2022-07-27 22:12:58.077Z\",\"updatedAt\":\"2022-07-27 22:12:58.077Z\",\"availableAt\":\"2022-07-27 22:12:58.077Z\",\"expiresAt\":\"2022-07-27 22:42:58.077Z\",\"refreshAt\":null,\"status\":\"active\",\"version\":0,\"ttl\":1800000,\"ttb\":0,\"ttr\":null,\"ccd\":null,\"isBinary\":false,\"isEncrypted\":true,\"dataSignature\":\"oIq0kHvwQieVrhOs4dJLN61qNP73bNLLNPTRW7tAdapIZF3kSMrNVCcTAWWWyzb2Tyii51uZ7zlIYmHWuS4tIE0lMzrUeXGcfQhOrdjkrxf4qEceNR1qLa7tDjOAb8xuhf/zJ3yaen8NGswfKWwQluga/52SchFClrR99xEI93s=\",\"sharedKeyEnc\":null,\"pubKeyCS\":null}";

        Secondary.Response response = new Secondary.Response();
        response.data = LLOOKUP_META_STR;

        LlookupMetadataResponseTransformer transformer = new LlookupMetadataResponseTransformer();
        Map<String, Object> map = transformer.transform(response);

        assertEquals(null, map.get("createdBy"));
        assertEquals(null, map.get("updatedBy"));
        assertEquals("2022-07-27 22:12:58.077Z", map.get("createdAt"));
        assertEquals("2022-07-27 22:12:58.077Z", map.get("updatedAt"));
        assertEquals("2022-07-27 22:12:58.077Z", map.get("availableAt"));
        assertEquals("2022-07-27 22:42:58.077Z", map.get("expiresAt"));
        assertEquals(null, map.get("refreshAt"));
        assertEquals("active", map.get("status"));
        assertEquals(0, map.get("version"));
        assertEquals(1800000, map.get("ttl"));
        assertEquals(0, map.get("ttb"));
        assertEquals(null, map.get("ttr"));
        assertEquals(null, map.get("ccd"));
        assertEquals(false, map.get("isBinary"));
        assertEquals(true, map.get("isEncrypted"));
        assertEquals("oIq0kHvwQieVrhOs4dJLN61qNP73bNLLNPTRW7tAdapIZF3kSMrNVCcTAWWWyzb2Tyii51uZ7zlIYmHWuS4tIE0lMzrUeXGcfQhOrdjkrxf4qEceNR1qLa7tDjOAb8xuhf/zJ3yaen8NGswfKWwQluga/52SchFClrR99xEI93s=", map.get("dataSignature"));
        assertEquals(null, map.get("sharedKeyEnc"));
        assertEquals(null, map.get("pubKeyCS"));
    }

}