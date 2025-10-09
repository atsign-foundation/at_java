package org.atsign.cucumber.steps;

import io.cucumber.java.After;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.atsign.common.AtException;
import org.atsign.common.AtSign;
import org.atsign.common.KeyBuilders;
import org.atsign.common.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class SharedAtKeySteps {

  private static final Logger LOGGER = LoggerFactory.getLogger(SharedAtKeySteps.class);

  private final AtClientContext context;

  private final Map<AtSign, Set<Keys.SharedKey>> keys = new HashMap<>();

  @After
  public void teardown() {
    int count = 0;
    for (Map.Entry<AtSign, Set<Keys.SharedKey>> entry : keys.entrySet()) {
      for (Keys.SharedKey key : entry.getValue()) {
        try {
          context.getAtClient(entry.getKey()).delete(key);
          count++;
        } catch (AtException e) {
          LOGGER.warn("unexpected exception attempting to delete shared key {}", key);
        }
      }
    }
    keys.clear();
    LOGGER.info("deleted {} shared keys", count);
  }

  public SharedAtKeySteps(AtClientContext context) {
    this.context = context;
  }

  @When("{atsign} AtClient.put for SharedKey {word} shared with {atsign} and value {string}")
  public void putSharedKey(AtSign atSign, String name, AtSign sharedWith, String value) throws Exception {
    Keys.SharedKey key = toKey(atSign, name, sharedWith);
    context.getAtClient(atSign).put(key, value).get();
    keys.computeIfAbsent(atSign, k -> new HashSet<>()).add(key);
  }

  @When("AtClient.put for SharedKey {word} shared with {atsign} and value {string}")
  public void putSharedKey(String name, AtSign sharedWith, String value) throws Exception {
    putSharedKey(context.getCurrentAtSign(), name, sharedWith, value);
  }

  @When("{atsign} AtClient.delete for SharedKey {word} shared with {atsign}")
  public void deleteSharedKey(AtSign atSign, String name, AtSign sharedWith) throws Exception {
    Keys.SharedKey key = toKey(atSign, name, sharedWith);
    context.getAtClient(atSign).delete(key).get();
    keys.computeIfAbsent(atSign, k -> new HashSet<>()).remove(key);
  }

  @When("AtClient.delete for SharedKey {word} shared with {atsign}")
  public void deleteSharedKey(String name, AtSign sharedWith) throws Exception {
    deleteSharedKey(context.getCurrentAtSign(), name, sharedWith);
  }

  @Then("{atsign} AtClient.get for SharedKey {word} shared with {atsign} returns value that matches {string}")
  public void assertGetSharedKeyResultForOwner(AtSign atSign, String name, AtSign sharedWith, String expected) throws Exception {
    String actual = context.getAtClient(atSign).get(toKey(atSign, name, sharedWith)).get();
    assertThat(actual, equalTo(expected));
  }

  @Then("{atsign} AtClient.get for SharedKey {word} shared by {atsign} returns value that matches {string}")
  public void assertGetSharedKeyResultForRecipient(AtSign atSign, String name, AtSign sharedBy, String expected) throws Exception {
    String actual = context.getAtClient(atSign).get(toKey(sharedBy, name, atSign)).get();
    assertThat(actual, equalTo(expected));
  }

  @Then("AtClient.get for SharedKey {word} shared with {atsign} returns value that matches {string}")
  public void assertGetSharedKeyResultForOwner(String name, AtSign sharedWith, String expected) throws Exception {
    assertGetSharedKeyResultForOwner(context.getCurrentAtSign(), name, sharedWith, expected);
  }

  @Then("AtClient.get for SharedKey {word} shared by {atsign} returns value that matches {string}")
  public void assertGetSharedKeyResultForRecipient(String name, AtSign sharedBy, String expected) throws Exception {
    assertGetSharedKeyResultForRecipient(context.getCurrentAtSign(), name, sharedBy, expected);
  }

  @Then("{atsign} AtClient.get for SharedKey {word} shared with {atsign} receives {exception} with message {string}")
  public void assertGetSharedKeyExceptionForOwner(AtSign atSign,
                                          String name,
                                          AtSign sharedWith,
                                          Class<AtException> expectedException,
                                          String expectedMessage) {
    Exception ex = assertThrows(Exception.class,
        () -> context.getAtClient(atSign).get(toKey(atSign, name, sharedWith)).get());
    assertThat(ex.getCause().getClass(), typeCompatibleWith(expectedException));
    assertThat(ex.getMessage(), containsString(expectedMessage));
  }

  @Then("AtClient.get for SharedKey {word} shared with {atsign} receives {exception} and message {string}")
  public void assertGetSharedKeyExceptionForOwner(String name,
                                          AtSign sharedWith,
                                          Class<AtException> expectedException,
                                          String expectedMessage) {
    assertGetSharedKeyExceptionForOwner(context.getCurrentAtSign(), name, sharedWith, expectedException, expectedMessage);
  }

  @Then("{atsign} AtClient.get for SharedKey {word} shared by {atsign} receives {exception} and message {string}")
  public void assertGetSharedKeyExceptionForRecipient(AtSign atSign,
                                          String name,
                                          AtSign sharedBy,
                                          Class<AtException> expectedException,
                                          String expectedMessage) {
    Exception ex = assertThrows(Exception.class,
        () -> context.getAtClient(atSign).get(toKey(sharedBy, name, atSign)).get());
    assertThat(ex.getCause().getClass(), typeCompatibleWith(expectedException));
    assertThat(ex.getMessage(), containsString(expectedMessage));
  }

  @Then("AtClient.get for SharedKey {word} shared by {atsign} receives {exception} with message {string}")
  public void assertGetSharedKeyExceptionForRecipient(String name,
                                          AtSign sharedBy,
                                          Class<AtException> expectedException,
                                          String expectedMessage) {
    assertGetSharedKeyExceptionForRecipient(context.getCurrentAtSign(), name, sharedBy, expectedException, expectedMessage);
  }

  private Keys.SharedKey toKey(AtSign atSign, String name, AtSign sharedWith) {
    Keys.SharedKey key = new KeyBuilders.SharedKeyBuilder(atSign, sharedWith).key(name).build();
    key.metadata.ttl = (int) context.getKeyTtl();
    return key;
  }
}
