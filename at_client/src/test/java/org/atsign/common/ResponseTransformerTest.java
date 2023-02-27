package org.atsign.common;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.atsign.client.api.Secondary;
import org.atsign.common.ResponseTransformers.LlookupAllResponseTransformer;
import org.atsign.common.ResponseTransformers.NotifyListResponseTransformer;
import org.atsign.common.response_models.LlookupAllResponse;
import org.atsign.common.response_models.NotifyListResponse;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class ResponseTransformerTest {

    @Before
    public void setUp() {
    }

    @Test
    public void scanResponseTransformerTest() {

    }

    @Test
    public void llookupAllResponseTransformerTest() {
        String RESPONSE_STR = "{" +
                "\"key\":\"public:publickey@cooking2\"," +
                "\"data\":\"MIGfMA0GCSqGSIb3DQEBAQUAA4GNADCBiQKBgQCz9nTBDDLxLgSxYu4+mDF3anWuTlKysXBBLsp3glrBP9xEXDx4muOHuHZIzuNvFlsjcCDF/mLSAJcvbxUoTsOQp+QD5XMhlNS9TWGsmNks7KHylNEhcqo2Va7RZxNS6MZBRacl+OusnebVKdOXDnbuevoED5fSklOz7mvdm9Mb2wIDAQAB\","
                +
                "\"metaData\":{" +
                "\"createdBy\":null," +
                "\"updatedBy\":null," +
                "\"createdAt\":\"2022-08-12 01:50:15.398Z\"," +
                "\"updatedAt\":\"2022-08-12 01:50:15.398Z\"," +
                "\"availableAt\":\"2022-08-12 01:50:15.398Z\"," +
                "\"expiresAt\":null," +
                "\"refreshAt\":null," +
                "\"status\":\"active\"," +
                "\"version\":0," +
                "\"ttl\":0," +
                "\"ttb\":0," +
                "\"ttr\":null," +
                "\"ccd\":null," +
                "\"isBinary\":false," +
                "\"isEncrypted\":false," +
                "\"dataSignature\":null," +
                "\"sharedKeyEnc\":null," +
                "\"pubKeyCS\":null," +
                "\"encoding\":null" +
                "}" +
                "}";

        // System.out.println(RESPONSE_STR);
        // //{"key":"public:publickey@cooking2","data":"MIGfMA0GCSqGSIb3DQEBAQUAA4GNADCBiQKBgQCz9nTBDDLxLgSxYu4+mDF3anWuTlKysXBBLsp3glrBP9xEXDx4muOHuHZIzuNvFlsjcCDF/mLSAJcvbxUoTsOQp+QD5XMhlNS9TWGsmNks7KHylNEhcqo2Va7RZxNS6MZBRacl+OusnebVKdOXDnbuevoED5fSklOz7mvdm9Mb2wIDAQAB","metaData":{"createdBy":null,"updatedBy":null,"createdAt":"2022-08-12
        // 01:50:15.398Z","updatedAt":"2022-08-12
        // 01:50:15.398Z","availableAt":"2022-08-12
        // 01:50:15.398Z","expiresAt":null,"refreshAt":null,"status":"active","version":0,"ttl":0,"ttb":0,"ttr":null,"ccd":null,"isBinary":false,"isEncrypted":false,"dataSignature":null,"sharedKeyEnc":null,"pubKeyCS":null,"encoding":null}}
        Secondary.Response response = new Secondary.Response();
        response.isError = false;
        response.data = RESPONSE_STR;
        LlookupAllResponse model = null;
        LlookupAllResponseTransformer transformer = new LlookupAllResponseTransformer();
        model = transformer.transform(response);

        assertEquals("public:publickey@cooking2", model.key);
        assertEquals(
                "MIGfMA0GCSqGSIb3DQEBAQUAA4GNADCBiQKBgQCz9nTBDDLxLgSxYu4+mDF3anWuTlKysXBBLsp3glrBP9xEXDx4muOHuHZIzuNvFlsjcCDF/mLSAJcvbxUoTsOQp+QD5XMhlNS9TWGsmNks7KHylNEhcqo2Va7RZxNS6MZBRacl+OusnebVKdOXDnbuevoED5fSklOz7mvdm9Mb2wIDAQAB",
                model.data);
        assertEquals(null, model.metaData.createdBy);
        assertEquals(null, model.metaData.updatedBy);
        assertEquals("2022-08-12 01:50:15.398Z", model.metaData.createdAt);
        assertEquals("2022-08-12 01:50:15.398Z", model.metaData.updatedAt);
        assertEquals("2022-08-12 01:50:15.398Z", model.metaData.availableAt);
        assertEquals(null, model.metaData.expiresAt);
        assertEquals(null, model.metaData.refreshAt);
        assertEquals("active", model.metaData.status);
        assertTrue(model.metaData.version == 0);
        assertTrue(model.metaData.ttl == 0);
        assertTrue(model.metaData.ttb == 0);
        assertEquals(null, model.metaData.ttr);
        assertEquals(null, model.metaData.ccd);
        assertEquals(false, model.metaData.isBinary);
        assertEquals(false, model.metaData.isEncrypted);
        assertEquals(null, model.metaData.dataSignature);
        assertEquals(null, model.metaData.sharedKeyEnc);
        assertEquals(null, model.metaData.pubKeyCS);
        assertEquals(null, model.metaData.encoding);

        // System.out.println("FullKeyName: " + model.key);
        // System.out.println("Data: " + model.data);
        // System.out.println("MetaData.createdBy: " + model.metaData.createdBy);
        // System.out.println("MetaData.updatedBy: " + model.metaData.updatedBy);
        // System.out.println("MetaData.createdAt: " + model.metaData.createdAt);
        // System.out.println("MetaData.updatedAt: " + model.metaData.updatedAt);
        // System.out.println("MetaData.availableAt: " + model.metaData.availableAt);
        // System.out.println("MetaData.expiresAt: " + model.metaData.expiresAt);
        // System.out.println("MetaData.refreshAt: " + model.metaData.refreshAt);
        // System.out.println("MetaData.status: " + model.metaData.status);
        // System.out.println("MetaData.version: " + model.metaData.version);
        // System.out.println("MetaData.ttl: " + model.metaData.ttl);
        // System.out.println("MetaData.ttb: " + model.metaData.ttb);
        // System.out.println("MetaData.ttr: " + model.metaData.ttr);
        // System.out.println("MetaData.ccd: " + model.metaData.ccd);
        // System.out.println("MetaData.isBinary: " + model.metaData.isBinary);
        // System.out.println("MetaData.isEncrypted: " + model.metaData.isEncrypted);
        // System.out.println("MetaData.dataSignature: " +
        // model.metaData.dataSignature);
        // System.out.println("MetaData.sharedKeyEnc: " + model.metaData.sharedKeyEnc);
        // System.out.println("MetaData.pubKeyCS: " + model.metaData.pubKeyCS);
        // System.out.println("MetaData.encoding: " + model.metaData.encoding);

    }

    @Test
    public void notifyListResponseTransformerTest() {
        /**
         * [
         * {
         * "id":"915a5c6a-314e-437f-b464-9a8c0d80770d",
         * "from":"@soccer0",
         * "to":"@22easy",
         * "key":"@22easy:12345",
         * "value":null,
         * "operation":"null"
         * "epochMillis":1671491608714,
         * "messageType":"MessageType.text",
         * "isEncrypted":false
         * },
         * {
         * "id":"c18d15ef-3da9-4538-b699-4a6542666678"
         * "from":"@soccer0",
         * "to":"@22easy",
         * "key":"@22easy:12345",
         * "value":null,
         * "operation":"null",
         * "epochMillis":1671491611414,
         * "messageType":"MessageType.text",
         * "isEncrypted":false
         * },
         * {
         * "id":"ce872442-a3e5-4624-b27c-f37a69e6bd3e",
         * "from":"@soccer0",
         * "to":"@22easy",
         * "key":"@22easy:12345",
         * "value":null,
         * "operation":"null",
         * "epochMillis":1671491603639,
         * "messageType":"MessageType.text",
         * "isEncrypted":false
         * }
         * ]
         */

        final String RESPONSE_STR = "[{\"id\":\"915a5c6a-314e-437f-b464-9a8c0d80770d\",\"from\":\"@soccer0\",\"to\":\"@22easy\",\"key\":\"@22easy:12345\",\"value\":null,\"operation\":\"null\",\"epochMillis\":1671491608714,\"messageType\":\"MessageType.text\",\"isEncrypted\":false},{\"id\":\"c18d15ef-3da9-4538-b699-4a6542666678\",\"from\":\"@soccer0\",\"to\":\"@22easy\",\"key\":\"@22easy:12345\",\"value\":null,\"operation\":\"null\",\"epochMillis\":1671491611414,\"messageType\":\"MessageType.text\",\"isEncrypted\":false},{\"id\":\"ce872442-a3e5-4624-b27c-f37a69e6bd3e\",\"from\":\"@soccer0\",\"to\":\"@22easy\",\"key\":\"@22easy:12345\",\"value\":null,\"operation\":\"null\",\"epochMillis\":1671491603639,\"messageType\":\"MessageType.text\",\"isEncrypted\":false}]";

        Secondary.Response response = new Secondary.Response();
        response.isError = false;
        response.data = RESPONSE_STR;

        NotifyListResponseTransformer transformer = new NotifyListResponseTransformer();
        NotifyListResponse model = transformer.transform(response);
        
        assertEquals(3, model.notifications.size());
        
        // check notification 1
        NotifyListResponse.Notification n1 = model.notifications.get(0);
        assertEquals("915a5c6a-314e-437f-b464-9a8c0d80770d", n1.id);
        assertEquals("@soccer0", n1.from);
        assertEquals("@22easy", n1.to);
        assertEquals("@22easy:12345", n1.key);
        assertEquals(null, n1.value);
        assertEquals("null", n1.operation);
        assertEquals(1671491608714L, n1.epochMillis.longValue());
        assertEquals("MessageType.text", n1.messageType);
        assertEquals(false, n1.isEncrypted);

        // check notification 2
        NotifyListResponse.Notification n2 = model.notifications.get(1);
        assertEquals("c18d15ef-3da9-4538-b699-4a6542666678", n2.id);
        assertEquals("@soccer0", n2.from);
        assertEquals("@22easy", n2.to);
        assertEquals("@22easy:12345", n2.key);
        assertEquals(null, n2.value);
        assertEquals("null", n2.operation);
        assertEquals(1671491611414L, n2.epochMillis.longValue());
        assertEquals("MessageType.text", n2.messageType);
        assertEquals(false, n2.isEncrypted);

        // check notification 3
        NotifyListResponse.Notification n3 = model.notifications.get(2);
        assertEquals("ce872442-a3e5-4624-b27c-f37a69e6bd3e", n3.id);
        assertEquals("@soccer0", n3.from);
        assertEquals("@22easy", n3.to);
        assertEquals("@22easy:12345", n3.key);
        assertEquals(null, n3.value);
        assertEquals("null", n3.operation);
        assertEquals(1671491603639L, n3.epochMillis.longValue());
        assertEquals("MessageType.text", n3.messageType);
        assertEquals(false, n3.isEncrypted);
    }

    @After
    public void tearDown() {
    }

}