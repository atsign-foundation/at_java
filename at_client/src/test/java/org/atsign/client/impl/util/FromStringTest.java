package org.atsign.client.impl.util;

import static org.atsign.client.api.AtSign.createAtSign;
import static org.junit.jupiter.api.Assertions.*;

import org.atsign.client.impl.exceptions.AtException;
import org.atsign.client.api.Keys;
import org.atsign.client.api.Keys.AtKey;
import org.atsign.client.api.Metadata;
import org.junit.jupiter.api.Test;

public class FromStringTest {


  @Test
  public void fromStringTest0() throws AtException {
    // no llookup meta
    AtKey atKey = Keys.keyBuilder().rawKey("public:publickey@alice").build();

    Metadata metadata = atKey.metadata();
    assertEquals(true, metadata.isPublic());
    assertEquals(false, metadata.isEncrypted());
    assertEquals(false, metadata.isHidden());
    assertEquals("publickey", atKey.name());
    assertEquals(createAtSign("@alice"), atKey.sharedBy());
  }

  @Test
  public void fromStringTest1() throws Exception {
    String KEY_NAME_STR = "public:publickey@bob";

    String response = "{\"createdBy\":null,\"updatedBy\":null,"
        + "\"createdAt\":\"2022-07-13 21:54:28.519Z\",\"updatedAt\":\"2022-07-13 21:54:28.519Z\","
        + "\"availableAt\":\"2022-07-13 21:54:28.519Z\",\"expiresAt\":null,"
        + "\"refreshAt\":null,\"status\":\"active\",\"version\":0," + "\"ttl\":0,\"ttb\":0,\"ttr\":null,\"ccd\":null,"
        + "\"isBinary\":false,\"isEncrypted\":false,"
        + "\"dataSignature\":null,\"sharedKeyEnc\":null,\"pubKeyCS\":null}";

    AtKey atKey = fromString(KEY_NAME_STR, response);

    assertEquals(KEY_NAME_STR, atKey.toString());
    Metadata metadata = atKey.metadata();
    assertEquals(true, metadata.isPublic());
    assertEquals("publickey", atKey.name());
    assertEquals("@bob", atKey.sharedBy().toString());
    assertEquals("2022-07-13T21:54:28.519Z", metadata.createdAt().toString());
    assertEquals("2022-07-13T21:54:28.519Z", metadata.updatedAt().toString());
    assertEquals("2022-07-13T21:54:28.519Z", metadata.availableAt().toString());
    assertNull(metadata.expiresAt());
    assertNull(metadata.refreshAt());
    assertEquals(0, metadata.ttl().intValue());
    assertEquals(0, metadata.ttb().intValue());
    assertNull(metadata.ttr());
    assertNull(metadata.ccd());
    assertEquals(false, metadata.isBinary());
    assertEquals(false, metadata.isEncrypted());
    assertNull(metadata.dataSignature());
    assertNull(metadata.sharedKeyEnc());
    assertNull(metadata.pubKeyCS());
  }

  @Test
  public void fromStringTest2() throws Exception {
    String KEY_NAME_STR = "test@bob";

    @SuppressWarnings("SpellCheckingInspection")
    String LLOOKUP_META_STR = "{\"createdBy\":null,\"updatedBy\":null,\"createdAt\":\"2022-07-27 22:12:58.077Z\","
        + "\"updatedAt\":\"2022-07-27 22:12:58.077Z\",\"availableAt\":\"2022-07-27 22:12:58.077Z\","
        + "\"expiresAt\":\"2022-07-27 22:42:58.077Z\",\"refreshAt\":null,\"status\":\"active\",\"version\":0,"
        + "\"ttl\":1800000,\"ttb\":0,\"ttr\":null,\"ccd\":null," + "\"isBinary\":false,\"isEncrypted\":true,"
        + "\"dataSignature\":\"oIq0kHvwQieVrhOs4dJLN61qNP73bNLLNPTRW7tAdapIZF3kSMrNVCcTAWWWyzb2Tyii51uZ7zlIYmHWuS4tIE0lMzrUeXGcfQhOrdjkrxf4qEceNR1qLa7tDjOAb8xuhf/zJ3yaen8NGswfKWwQluga/52SchFClrR99xEI93s=\","
        + "\"sharedKeyEnc\":null,\"pubKeyCS\":null}";

    AtKey atKey = fromString(KEY_NAME_STR, LLOOKUP_META_STR);

    assertEquals(KEY_NAME_STR, atKey.toString());
    Metadata metadata = atKey.metadata();
    assertEquals(false, metadata.isPublic());
    assertEquals("test", atKey.name());
    assertEquals("@bob", atKey.sharedBy().toString());
    assertEquals("2022-07-27T22:12:58.077Z", metadata.createdAt().toString());
    assertEquals("2022-07-27T22:12:58.077Z", metadata.updatedAt().toString());
    assertEquals("2022-07-27T22:12:58.077Z", metadata.availableAt().toString());
    assertEquals("2022-07-27T22:42:58.077Z", metadata.expiresAt().toString());
    assertNull(metadata.refreshAt());
    assertEquals(1800000L, metadata.ttl());
    assertEquals(0L, metadata.ttb());
    assertNull(metadata.ttr());
    assertNull(metadata.ccd());
    assertFalse(metadata.isBinary());
    assertTrue(metadata.isEncrypted());
    // noinspection SpellCheckingInspection
    assertEquals(
                 "oIq0kHvwQieVrhOs4dJLN61qNP73bNLLNPTRW7tAdapIZF3kSMrNVCcTAWWWyzb2Tyii51uZ7zlIYmHWuS4tIE0lMzrUeXGcfQhOrdjkrxf4qEceNR1qLa7tDjOAb8xuhf/zJ3yaen8NGswfKWwQluga/52SchFClrR99xEI93s=",
                 metadata.dataSignature());
    assertNull(metadata.sharedKeyEnc());
    assertNull(metadata.pubKeyCS());
  }

  public static AtKey fromString(String rawKey, String metaDataJson) throws Exception {
    Metadata metadata = Metadata.fromJson(metaDataJson);
    return Keys.keyBuilder()
        .rawKey(rawKey)
        .metadata(metadata)
        .build();
  }
}
