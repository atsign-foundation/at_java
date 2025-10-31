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

public class SelfAtKeySteps {

  private static final Logger LOGGER = LoggerFactory.getLogger(SelfAtKeySteps.class);

  private final AtClientContext context;

  private final Map<AtSign, Set<Keys.SelfKey>> keys = new HashMap<>();

  @After
  public void teardown() {
    int count = 0;
    for (Map.Entry<AtSign, Set<Keys.SelfKey>> entry : keys.entrySet()) {
      for (Keys.SelfKey key : entry.getValue()) {
        try {
          context.getAtClient(entry.getKey()).delete(key);
          count++;
        } catch (AtException e) {
          LOGGER.warn("unexpected exception attempting to delete self key {}", key);
        }
      }
    }
    keys.clear();
    LOGGER.info("deleted {} self keys", count);
  }

  public SelfAtKeySteps(AtClientContext context) {
    this.context = context;
  }

  @When("{atsign} AtClient.put for SelfKey {word} and value {string}")
  public void putSelfKey(AtSign atSign, String name, String value) throws Exception {
    Keys.SelfKey key = toKey(atSign, name);
    context.getAtClient(atSign).put(key, value).get();
    keys.computeIfAbsent(atSign, k -> new HashSet<>()).add(key);
  }

  @When("AtClient.put for SelfKey {word} and value {string}")
  public void putSelfKey(String name, String value) throws Exception {
    putSelfKey(context.getCurrentAtSign(), name, value);
  }

  @When("{atsign} AtClient.delete for SelfKey {word}")
  public void deleteSelfKey(AtSign atSign, String name) throws Exception {
    Keys.SelfKey key = toKey(atSign, name);
    context.getAtClient(atSign).delete(key).get();
    keys.computeIfAbsent(atSign, k -> new HashSet<>()).remove(key);
  }

  @When("AtClient.delete for SelfKey {word}")
  public void deleteSelfKey(String name) throws Exception {
    deleteSelfKey(context.getCurrentAtSign(), name);
  }

  @Then("{atsign} AtClient.get for SelfKey {word} returns value that matches {string}")
  public void assertGetSelfKeyResult(AtSign atSign, String name, String expected) throws Exception {
    String actual = context.getAtClient(atSign).get(toKey(atSign, name)).get();
    assertThat(actual, equalTo(expected));
  }

  @Then("AtClient.get for SelfKey {word} returns value that matches {string}")
  public void assertGetSelfKeyResult(String name, String expected) throws Exception {
    assertGetSelfKeyResult(context.getCurrentAtSign(), name, expected);
  }

  @Then("{atsign} AtClient.get for SelfKey {word} receives {exception} and message {string}")
  public void assertGetSelfKeyException(AtSign atSign,
                                        String name,
                                        Class<AtException> expectedException,
                                        String expectedMessage) {
    Exception ex = assertThrows(Exception.class,
        () -> context.getAtClient(atSign).get(toKey(atSign, name)).get());
    assertThat(ex.getCause().getClass(), typeCompatibleWith(expectedException));
    assertThat(ex.getMessage(), containsString(expectedMessage));
  }

  @Then("AtClient.get for SelfKey {word} receives {exception} and message {string}")
  public void assertGetSelfKeyException(String name,
                                        Class<AtException> expectedException,
                                        String expectedMessage) {
    assertGetSelfKeyException(context.getCurrentAtSign(), name, expectedException, expectedMessage);
  }

  private Keys.SelfKey toKey(AtSign atSign, String name) {
    Keys.SelfKey key = new KeyBuilders.SelfKeyBuilder(atSign).key(name).build();
    key.metadata.ttl = (int) context.getKeyTtl();
    return key;
  }
}
