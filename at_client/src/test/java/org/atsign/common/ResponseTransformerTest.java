package org.atsign.common;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.atsign.client.api.Secondary;
import org.atsign.common.ResponseTransformers.LlookupAllResponseTransformer;
import org.atsign.common.response_models.LlookupAllResponse;
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
            "\"data\":\"MIGfMA0GCSqGSIb3DQEBAQUAA4GNADCBiQKBgQCz9nTBDDLxLgSxYu4+mDF3anWuTlKysXBBLsp3glrBP9xEXDx4muOHuHZIzuNvFlsjcCDF/mLSAJcvbxUoTsOQp+QD5XMhlNS9TWGsmNks7KHylNEhcqo2Va7RZxNS6MZBRacl+OusnebVKdOXDnbuevoED5fSklOz7mvdm9Mb2wIDAQAB\"," +
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

        // System.out.println(RESPONSE_STR); //{"key":"public:publickey@cooking2","data":"MIGfMA0GCSqGSIb3DQEBAQUAA4GNADCBiQKBgQCz9nTBDDLxLgSxYu4+mDF3anWuTlKysXBBLsp3glrBP9xEXDx4muOHuHZIzuNvFlsjcCDF/mLSAJcvbxUoTsOQp+QD5XMhlNS9TWGsmNks7KHylNEhcqo2Va7RZxNS6MZBRacl+OusnebVKdOXDnbuevoED5fSklOz7mvdm9Mb2wIDAQAB","metaData":{"createdBy":null,"updatedBy":null,"createdAt":"2022-08-12 01:50:15.398Z","updatedAt":"2022-08-12 01:50:15.398Z","availableAt":"2022-08-12 01:50:15.398Z","expiresAt":null,"refreshAt":null,"status":"active","version":0,"ttl":0,"ttb":0,"ttr":null,"ccd":null,"isBinary":false,"isEncrypted":false,"dataSignature":null,"sharedKeyEnc":null,"pubKeyCS":null,"encoding":null}}
        Secondary.Response response = new Secondary.Response();
        response.isError = false;
        response.data = RESPONSE_STR;
        LlookupAllResponse model = null;
        LlookupAllResponseTransformer transformer = new LlookupAllResponseTransformer();
        model = transformer.transform(response);

        assertEquals("public:publickey@cooking2", model.key);
        assertEquals("MIGfMA0GCSqGSIb3DQEBAQUAA4GNADCBiQKBgQCz9nTBDDLxLgSxYu4+mDF3anWuTlKysXBBLsp3glrBP9xEXDx4muOHuHZIzuNvFlsjcCDF/mLSAJcvbxUoTsOQp+QD5XMhlNS9TWGsmNks7KHylNEhcqo2Va7RZxNS6MZBRacl+OusnebVKdOXDnbuevoED5fSklOz7mvdm9Mb2wIDAQAB", model.data);
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
        // System.out.println("MetaData.dataSignature: " + model.metaData.dataSignature);
        // System.out.println("MetaData.sharedKeyEnc: " + model.metaData.sharedKeyEnc);
        // System.out.println("MetaData.pubKeyCS: " + model.metaData.pubKeyCS);
        // System.out.println("MetaData.encoding: " + model.metaData.encoding);

    }

    @After
    public void tearDown() {
    }

}