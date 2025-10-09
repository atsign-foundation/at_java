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

public class PublicAtKeySteps {

  private static final Logger LOGGER = LoggerFactory.getLogger(PublicAtKeySteps.class);

  private final AtClientContext context;

  private final Map<AtSign, Set<Keys.PublicKey>> keys = new HashMap<>();

  @After
  public void teardown() {
    int count = 0;
    for (Map.Entry<AtSign, Set<Keys.PublicKey>> entry : keys.entrySet()) {
      for (Keys.PublicKey key : entry.getValue()) {
        try {
          context.getAtClient(entry.getKey()).delete(key);
          count++;
        } catch (AtException e) {
          LOGGER.warn("unexpected exception attempting to delete public key {}", key);
        }
      }
    }
    keys.clear();
    LOGGER.info("deleted {} self keys", count);
  }

  public PublicAtKeySteps(AtClientContext context) {
    this.context = context;
  }

  @When("{atsign} AtClient.put for PublicKey {word} and value {string}")
  public void putPublicKey(AtSign atSign, String name, String value) throws Exception {
    Keys.PublicKey key = toKey(atSign, name);
    context.getAtClient(atSign).put(key, value).get();
    keys.computeIfAbsent(atSign, k -> new HashSet<>()).add(key);
  }

  @When("AtClient.put for PublicKey {word} and value {string}")
  public void putPublicKey(String name, String value) throws Exception {
    putPublicKey(context.getCurrentAtSign(), name, value);
  }

  @When("{atsign} AtClient.delete for PublicKey {word}")
  public void deletePublicKey(AtSign atSign, String name) throws Exception {
    Keys.PublicKey key = toKey(atSign, name);
    context.getAtClient(atSign).delete(key).get();
    keys.computeIfAbsent(atSign, k -> new HashSet<>()).remove(key);
  }

  @When("AtClient.delete for PublicKey {word}")
  public void deletePublicKey(String name) throws Exception {
    deletePublicKey(context.getCurrentAtSign(), name);
  }

  @Then("{atsign} AtClient.get for PublicKey {word} returns value that matches {string}")
  public void assertGetPublicKeyResult(AtSign atSign, String name, String expected) throws Exception {
    String actual = context.getAtClient(atSign).get(toKey(atSign, name)).get();
    assertThat(actual, equalTo(expected));
  }

  @Then("{atsign} AtClient.get for PublicKey {word} shared by {atsign} returns value that matches {string}")
  public void assertGetPublicKeyResult(AtSign atSign, String name, AtSign sharedBy, String expected) throws Exception {
    String actual = context.getAtClient(atSign).get(toKey(sharedBy, name)).get();
    assertThat(actual, equalTo(expected));
  }

  @Then("AtClient.get for PublicKey {word} returns value that matches {string}")
  public void assertGetPublicKeyResult(String name, String expected) throws Exception {
    assertGetPublicKeyResult(context.getCurrentAtSign(), name, expected);
  }

  @Then("{atsign} AtClient.get for PublicKey {word} receives {exception} and message {string}")
  public void assertGetPublicKeyException(AtSign atSign,
                                        String name,
                                        Class<AtException> expectedException,
                                        String expectedMessage) {
    Exception ex = assertThrows(Exception.class,
        () -> context.getAtClient(atSign).get(toKey(atSign, name)).get());
    assertThat(ex.getCause().getClass(), typeCompatibleWith(expectedException));
    assertThat(ex.getMessage(), containsString(expectedMessage));
  }

  @Then("AtClient.get for PublicKey {word} receives {exception} and message {string}")
  public void assertGetPublicKeyException(String name,
                                        Class<AtException> expectedException,
                                        String expectedMessage) {
    assertGetPublicKeyException(context.getCurrentAtSign(), name, expectedException, expectedMessage);
  }

  private Keys.PublicKey toKey(AtSign atSign, String name) {
    Keys.PublicKey key = new KeyBuilders.PublicKeyBuilder(atSign).key(name).build();
    key.metadata.ttl = (int) context.getKeyTtl();
    return key;
  }
}
