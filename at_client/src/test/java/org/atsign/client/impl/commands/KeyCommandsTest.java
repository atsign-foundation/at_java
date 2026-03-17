package org.atsign.client.impl.commands;

import static org.atsign.client.api.AtSign.createAtSign;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;

import java.util.List;

import org.atsign.client.api.AtCommandExecutor;
import org.junit.jupiter.api.Test;

class KeyCommandsTest {

  @Test
  void testDeleteKeyRawKey() throws Exception {
    AtCommandExecutor executor = TestExecutorBuilder.builder()
        .stub("delete:selfkey1@alice", "data:1")
        .build();

    KeyCommands.deleteKey(executor, "selfkey1@alice");
  }

  @Test
  void testDeleteKeyRawKeyThrowsExceptionIfResponseIsError() throws Exception {
    AtCommandExecutor executor = TestExecutorBuilder.builder()
        .stub("delete:selfkey1@alice", "error:AT0001:an error message")
        .build();

    assertThrows(Exception.class, () -> KeyCommands.deleteKey(executor, "selfkey1@alice"));
  }

  @Test
  void testDeleteKey() throws Exception {
    AtCommandExecutor executor = TestExecutorBuilder.builder()
        .stub("delete:keyname@colin", "data:1")
        .build();

    org.atsign.client.api.Keys.SelfKey key = org.atsign.client.api.Keys.selfKeyBuilder()
        .name("keyname")
        .sharedBy(createAtSign("colin"))
        .build();
    KeyCommands.deleteKey(executor, key);

    verify(executor).sendSync("delete:keyname@colin");
  }

  @Test
  void getKeysWithMetaDataReturnsExpectedResults() throws Exception {

    AtCommandExecutor executor = TestExecutorBuilder.builder()
        .stub("scan:showHidden:true .+", "data:[\"public:publickey@gary\",\"public:signing_publickey@gary\"]")
        .stub("llookup:meta:public:publickey@gary", "data:{\"ttl\":1000}")
        .stub("llookup:meta:public:signing_publickey@gary", "data:{\"ttl\":2000}")
        .build();

    List<org.atsign.client.api.Keys.AtKey> result = KeyCommands.getKeys(executor, ".+", true);

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

    AtCommandExecutor executor = TestExecutorBuilder.builder()
        .stub("scan:showHidden:true .+", "data:[\"public:publickey@gary\",\"public:signing_publickey@gary\"]")
        .build();

    List<org.atsign.client.api.Keys.AtKey> result = KeyCommands.getKeys(executor, ".*gary", false);

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
