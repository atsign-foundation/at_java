package org.atsign.common;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.atsign.client.api.Secondary;
import org.atsign.common.response_models.LookupResponse;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.time.format.DateTimeFormatter;

import static org.junit.Assert.*;

public class ResponseTransformerTest {
    final ObjectMapper mapper = new ObjectMapper();
    final DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS'Z'");


    @Before
    public void setUp() {
    }

    @Test
    public void scanResponseTransformerTest() {

    }

    @Test
    public void llookupAllResponseTransformerTest() throws JsonProcessingException {
        //noinspection SpellCheckingInspection
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

        Secondary.Response response = new Secondary.Response();
        response.setRawDataResponse(RESPONSE_STR);
        LookupResponse model = mapper.readValue(response.getRawDataResponse(), LookupResponse.class);

        assertEquals("public:publickey@cooking2", model.key);
        //noinspection SpellCheckingInspection
        assertEquals("MIGfMA0GCSqGSIb3DQEBAQUAA4GNADCBiQKBgQCz9nTBDDLxLgSxYu4+mDF3anWuTlKysXBBLsp3glrBP9xEXDx4muOHuHZIzuNvFlsjcCDF/mLSAJcvbxUoTsOQp+QD5XMhlNS9TWGsmNks7KHylNEhcqo2Va7RZxNS6MZBRacl+OusnebVKdOXDnbuevoED5fSklOz7mvdm9Mb2wIDAQAB", model.data);
        assertNull(model.metaData.createdBy);
        assertNull(model.metaData.updatedBy);

        assertEquals("2022-08-12 01:50:15.398Z", dateTimeFormatter.format(model.metaData.createdAt));
        assertEquals("2022-08-12 01:50:15.398Z", dateTimeFormatter.format(model.metaData.updatedAt));
        assertEquals("2022-08-12 01:50:15.398Z", dateTimeFormatter.format(model.metaData.availableAt));
        assertNull(model.metaData.expiresAt);
        assertNull(model.metaData.refreshAt);
        assertEquals("active", model.metaData.status);
        assertEquals(0, (int) model.metaData.version);
        assertEquals(0, (int) model.metaData.ttl);
        assertEquals(0, (int) model.metaData.ttb);
        assertNull(model.metaData.ttr);
        assertNull(model.metaData.ccd);
        assertFalse(model.metaData.isBinary);
        assertFalse(model.metaData.isEncrypted);
        assertNull(model.metaData.dataSignature);
        assertNull(model.metaData.sharedKeyEnc);
        assertNull(model.metaData.pubKeyCS);
        assertNull(model.metaData.encoding);
    }

    @After
    public void tearDown() {
    }

}
