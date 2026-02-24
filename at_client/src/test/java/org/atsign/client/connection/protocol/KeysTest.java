package org.atsign.client.connection.protocol;

import static org.atsign.common.AtSign.createAtSign;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;

import java.util.List;

import org.atsign.client.connection.api.AtClientConnection;
import org.junit.jupiter.api.Test;

class KeysTest {

  @Test
  void testDeleteKeyRawKey() throws Exception {
    AtClientConnection connection = TestConnectionBuilder.builder()
        .stub("delete:selfkey1@alice", "data:1")
        .build();

    Keys.deleteKey(connection, "selfkey1@alice");
  }

  @Test
  void testDeleteKeyRawKeyThrowsExceptionIfResponseIsError() throws Exception {
    AtClientConnection connection = TestConnectionBuilder.builder()
        .stub("delete:selfkey1@alice", "error:AT0001:an error message")
        .build();

    assertThrows(Exception.class, () -> Keys.deleteKey(connection, "selfkey1@alice"));
  }

  @Test
  void testDeleteKey() throws Exception {
    AtClientConnection connection = TestConnectionBuilder.builder()
        .stub("delete:keyname@colin", "data:1")
        .build();

    org.atsign.common.Keys.SelfKey key = org.atsign.common.Keys.selfKeyBuilder()
        .name("keyname")
        .sharedBy(createAtSign("colin"))
        .build();
    Keys.deleteKey(connection, key);

    verify(connection).sendSync("delete:keyname@colin");
  }

  @Test
  void getKeysWithMetaDataReturnsExpectedResults() throws Exception {

    AtClientConnection connection = TestConnectionBuilder.builder()
        .stub("scan:showHidden:true .+", "data:[\"public:publickey@gary\",\"public:signing_publickey@gary\"]")
        .stub("llookup:meta:public:publickey@gary", "data:{\"ttl\":1000}")
        .stub("llookup:meta:public:signing_publickey@gary", "data:{\"ttl\":2000}")
        .build();

    List<org.atsign.common.Keys.AtKey> result = Keys.getKeys(connection, ".+", true);

    assertThat(result.size(), equalTo(2));
    assertThat(result.get(0).rawKey(), equalTo("public:publickey@gary"));
    assertThat(result.get(0).name(), equalTo("publickey"));
    assertThat(result.get(0).sharedBy(), equalTo(createAtSign("gary")));
    assertThat(result.get(0).metadata().ttl(), equalTo(1000L));

    assertThat(result.get(1).rawKey(), equalTo("public:signing_publickey@gary"));
    assertThat(result.get(1).name(), equalTo("signing_publickey"));
    assertThat(result.get(1).sharedBy(), equalTo(createAtSign("gary")));
    assertThat(result.get(1).metadata().ttl(), equalTo(2000L));
  }

  @Test
  void getKeysWithoutMetaDataReturnsExpectedResults() throws Exception {

    AtClientConnection connection = TestConnectionBuilder.builder()
        .stub("scan:showHidden:true .+", "data:[\"public:publickey@gary\",\"public:signing_publickey@gary\"]")
        .build();

    List<org.atsign.common.Keys.AtKey> result = Keys.getKeys(connection, ".*gary", false);

    assertThat(result.size(), equalTo(2));
    assertThat(result.get(0).rawKey(), equalTo("public:publickey@gary"));
    assertThat(result.get(0).name(), equalTo("publickey"));
    assertThat(result.get(0).sharedBy(), equalTo(createAtSign("gary")));
    assertThat(result.get(0).metadata().ttl(), nullValue());

    assertThat(result.get(1).rawKey(), equalTo("public:signing_publickey@gary"));
    assertThat(result.get(1).name(), equalTo("signing_publickey"));
    assertThat(result.get(1).sharedBy(), equalTo(createAtSign("gary")));
    assertThat(result.get(1).metadata().ttl(), nullValue());
  }

}
