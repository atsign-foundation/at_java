package org.atsign.client.impl.commands.builders;

import static org.atsign.client.impl.util.EncryptionUtils.generateRSAKeyPair;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.atsign.client.api.Keys;
import org.atsign.client.impl.util.EncryptionUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.atsign.client.api.AtSign;

class NotifyUpdateSharedKeyCommandBuilderTest {

  private String sharedWithPublicKey;
  private Keys.SharedKey key;
  private NotifyUpdateSharedKeyCommandBuilder builder;

  @BeforeEach
  void setup() throws Exception {
    sharedWithPublicKey = EncryptionUtils.toStringBase64(generateRSAKeyPair().getPublic());
    key = Keys.sharedKeyBuilder()
        .name("key1")
        .sharedBy(AtSign.of("alice"))
        .sharedWith(AtSign.of("bob"))
        .build();
    builder = new NotifyUpdateSharedKeyCommandBuilder()
        .key(key)
        .sharedWithPublicKey(sharedWithPublicKey);
  }

  @Test
  void testBuildThrowsExceptionIfSharedWithPublicKeyNotSet() {
    NotifyUpdateSharedKeyCommandBuilder builder = new NotifyUpdateSharedKeyCommandBuilder();
    IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> builder.build("message"));
    assertThat(ex.getMessage(), containsString("sharedWithPublicKey not set"));
  }

  @Test
  void testBuildThrowsExceptionIfKeyNotSet() throws Exception {
    String sharedWithPublicKey = EncryptionUtils.toStringBase64(generateRSAKeyPair().getPublic());
    NotifyUpdateSharedKeyCommandBuilder builder = new NotifyUpdateSharedKeyCommandBuilder();
    IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                                               () -> builder.sharedWithPublicKey(sharedWithPublicKey).build("message"));
    assertThat(ex.getMessage(), containsString("key not set"));
  }

  @Test
  void testBuildGeneratesExpectedCommand() throws Exception {
    String command = builder.build("message");
    String expected = new StringBuilder()
        .append("notify:id:.+:update:messageType:key")
        .append(":notifier:SYSTEM:ttln:\\d+")
        .append(":isEncrypted:true:sharedKeyEnc:.+:pubKeyCS:.+:pubKeyHash:.+:hashingAlgo:sha512:ivNonce:.+")
        .append(":" + key.rawKey())
        .append(":(.+)")
        .toString();
    assertThat(command, matchesPattern(expected));
  }

  @Test
  void testUniqueId() throws Exception {
    Set<String> ids = new HashSet<>();
    Matcher idMatcher = Pattern.compile("notify:id:([^:]+)").matcher("");

    idMatcher.reset(builder.build("message"));
    assertThat(idMatcher.find(), is(true));
    assertThat(ids.add(idMatcher.group(1)), is(true));

    idMatcher.reset(builder.build("message"));
    assertThat(idMatcher.find(), is(true));
    assertThat(ids.add(idMatcher.group(1)), is(true));

    idMatcher.reset(builder.build("a different message"));
    assertThat(idMatcher.find(), is(true));
    assertThat(ids.add(idMatcher.group(1)), is(true));
  }

  @Test
  void testThatByDefaultSharedKeyIsUniqueForEachBuild() throws Exception {
    Set<String> sharedKeyEncs = new HashSet<>();
    Matcher sharedKeyEncMatcher = Pattern.compile("sharedKeyEnc:([^:]+)").matcher("");

    sharedKeyEncMatcher.reset(builder.build("message"));
    assertThat(sharedKeyEncMatcher.find(), is(true));
    assertThat(sharedKeyEncs.add(sharedKeyEncMatcher.group(1)), is(true));

    sharedKeyEncMatcher.reset(builder.build("message"));
    assertThat(sharedKeyEncMatcher.find(), is(true));
    assertThat(sharedKeyEncs.add(sharedKeyEncMatcher.group(1)), is(true));

    sharedKeyEncMatcher.reset(builder.build("a different message"));
    assertThat(sharedKeyEncMatcher.find(), is(true));
    assertThat(sharedKeyEncs.add(sharedKeyEncMatcher.group(1)), is(true));
  }


  @Test
  void testThatSharedKeyIsNotUniqueForEachBuildWhenReuseSharedKeyIsTrue() throws Exception {
    builder.reuseSharedKey(true);
    Set<String> sharedKeyEncs = new HashSet<>();
    Matcher sharedKeyEncMatcher = Pattern.compile("sharedKeyEnc:([^:]+)").matcher("");

    sharedKeyEncMatcher.reset(builder.build("message"));
    assertThat(sharedKeyEncMatcher.find(), is(true));
    assertThat(sharedKeyEncs.add(sharedKeyEncMatcher.group(1)), is(true));

    sharedKeyEncMatcher.reset(builder.build("message"));
    assertThat(sharedKeyEncMatcher.find(), is(true));
    assertThat(sharedKeyEncs.add(sharedKeyEncMatcher.group(1)), is(false));

    sharedKeyEncMatcher.reset(builder.build("a different message"));
    assertThat(sharedKeyEncMatcher.find(), is(true));
    assertThat(sharedKeyEncs.add(sharedKeyEncMatcher.group(1)), is(false));
  }

}
