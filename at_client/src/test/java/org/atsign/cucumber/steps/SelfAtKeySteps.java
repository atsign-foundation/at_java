package org.atsign.cucumber.steps;

import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.atsign.client.api.AtClient;
import org.atsign.client.util.KeyStringUtil;
import org.atsign.common.AtSign;
import org.atsign.common.KeyBuilders;
import org.atsign.common.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ExecutionException;
import java.util.regex.Matcher;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

public class SelfAtKeySteps {

  private static final Logger LOGGER = LoggerFactory.getLogger(SelfAtKeySteps.class);

  private final AtClientContext context;

  public SelfAtKeySteps(AtClientContext context) {
    this.context = context;
  }

  // put

  @When("{ordinal} {atsign} AtClient.put for SelfKey {word} and value {string}")
  public void put(Integer ordinal, AtSign clientAtSign, String name, String value) throws Exception {
    AtClient atClient = context.lookupAtClient(clientAtSign, ordinal);
    putKeyValue(atClient, clientAtSign, name, value);
  }

  @When("{ordinal} {atsign} AtClient.put fails for SelfKey {word} and value {string}")
  public void putFails(Integer ordinal, AtSign clientAtSign, String name, String value) throws Exception {
    AtClient atClient = context.lookupAtClient(clientAtSign, ordinal);
    context.assertException(() -> putKeyValue(atClient, clientAtSign, name, value));
  }

  @When("{atsign} AtClient.put for SelfKey {word} and value {string}")
  public void put(AtSign clientAtSign, String name, String value) throws Exception {
    AtClient atClient = context.lookupOrCreateAtClient(clientAtSign);
    putKeyValue(atClient, clientAtSign, name, value);
  }

  @When("AtClient.put for SelfKey {word} and value {string}")
  public void put(String name, String value) throws Exception {
    QualifiedAtSign currentQualifiedAtSign = context.getCurrentQualifiedAtSign();
    AtClient atClient = context.lookupAtClient(currentQualifiedAtSign);
    putKeyValue(atClient, currentQualifiedAtSign.getAtSign(), name, value);
  }

  // delete

  @When("{ordinal} {atsign} AtClient.delete for SelfKey {word}")
  public void delete(Integer ordinal, AtSign clientAtSign, String name) throws Exception {
    AtClient atClient = context.lookupAtClient(clientAtSign, ordinal);
    deleteKeyValue(atClient, clientAtSign, name);
  }

  @Then("{ordinal} {atsign} AtClient.delete fails for SelfKey {word}")
  public void deleteFails(Integer ordinal, AtSign clientAtSign, String name) throws Exception {
    AtClient atClient = context.lookupAtClient(clientAtSign, ordinal);
    context.assertException(() -> deleteKeyValue(atClient, clientAtSign, name));
  }

  @When("{atsign} AtClient.delete for SelfKey {word}")
  public void delete(AtSign clientAtSign, String name) throws Exception {
    AtClient atClient = context.lookupOrCreateAtClient(clientAtSign);
    deleteKeyValue(atClient, clientAtSign, name);
  }

  @Then("{atsign} AtClient.delete fails for SelfKey {word}")
  public void deleteFails(AtSign clientAtSign, String name) throws Exception {
    AtClient atClient = context.lookupOrCreateAtClient(clientAtSign);
    context.assertException(() -> deleteKeyValue(atClient, clientAtSign, name));
  }

  @When("AtClient.delete for SelfKey {word}")
  public void delete(String name) throws Exception {
    QualifiedAtSign currentQualifiedAtSign = context.getCurrentQualifiedAtSign();
    AtClient atClient = context.lookupAtClient(currentQualifiedAtSign);
    deleteKeyValue(atClient, currentQualifiedAtSign.getAtSign(), name);
  }

  @Then("AtClient.delete fails for SelfKey {word}")
  public void deleteFails(String name) throws Exception {
    QualifiedAtSign currentQualifiedAtSign = context.getCurrentQualifiedAtSign();
    AtClient atClient = context.lookupAtClient(currentQualifiedAtSign);
    context.assertException(() -> deleteKeyValue(atClient, currentQualifiedAtSign.getAtSign(), name));
  }

  // get

  @Then("{ordinal} {atsign} AtClient.get for SelfKey {word} returns value that matches {string}")
  public void assertGet(Integer ordinal, AtSign clientAtSign, String name, String expected) throws Exception {
    AtClient atClient = context.lookupAtClient(clientAtSign, ordinal);
    String keyValue = getKeyValue(atClient, clientAtSign, name);
    assertThat(keyValue, equalTo(expected));
  }

  @Then("{ordinal} {atsign} AtClient.get fails for SelfKey {word}")
  public void assertGetFails(Integer ordinal, AtSign clientAtSign, String name) throws Exception {
    AtClient atClient = context.lookupAtClient(clientAtSign, ordinal);
    context.assertException(() -> getKeyValue(atClient, clientAtSign, name));
  }

  @Then("{atsign} AtClient.get for SelfKey {word} returns value that matches {string}")
  public void assertGet(AtSign clientAtSign, String name, String expected) throws Exception {
    AtClient atClient = context.lookupOrCreateAtClient(clientAtSign);
    String keyValue = getKeyValue(atClient, clientAtSign, name);
    assertThat(keyValue, equalTo(expected));
  }

  @Then("{atsign} AtClient.get fails for SelfKey {word}")
  public void assertGetFails(AtSign clientAtSign, String name) throws Exception {
    AtClient atClient = context.lookupOrCreateAtClient(clientAtSign);
    context.assertException(() -> getKeyValue(atClient, clientAtSign, name));
  }

  @Then("AtClient.get for SelfKey {word} returns value that matches {string}")
  public void assertGet(String name, String expected) throws Exception {
    QualifiedAtSign currentQualifiedAtSign = context.getCurrentQualifiedAtSign();
    AtClient atClient = context.lookupAtClient(currentQualifiedAtSign);
    String keyValue = getKeyValue(atClient, currentQualifiedAtSign.getAtSign(), name);
    assertThat(keyValue, equalTo(expected));
  }

  @Then("AtClient.get fails for SelfKey {word}")
  public void assertGetFails(String name) throws Exception {
    QualifiedAtSign currentQualifiedAtSign = context.getCurrentQualifiedAtSign();
    AtClient atClient = context.lookupAtClient(currentQualifiedAtSign);
    context.assertException(() -> getKeyValue(atClient, currentQualifiedAtSign.getAtSign(), name));
  }

  private void putKeyValue(AtClient atClient, AtSign atSign, String name, String value)
      throws InterruptedException, ExecutionException {
    Keys.SelfKey key = createKey(atSign, name);
    atClient.put(key, value).get();
  }

  private String getKeyValue(AtClient atClient, AtSign atSign, String name) throws Exception {
    Keys.SelfKey key = createKey(atSign, name);
    return atClient.get(key).get();
  }

  private void deleteKeyValue(AtClient atClient, AtSign atSign, String name) throws Exception {
    Keys.SelfKey key = createKey(atSign, name);
    atClient.delete(key).get();
  }

  private Keys.SelfKey createKey(AtSign owner, String s) {
    KeyBuilders.SelfKeyBuilder builder = new KeyBuilders.SelfKeyBuilder(owner);
    Matcher matcher = KeyStringUtil.createNamespaceQualifiedKeyNameMatcher(s);
    if (matcher.matches()) {
      if (context.isNamespaceSet()) {
        throw new IllegalArgumentException("context has namespace set, intention is ambiguous");
      }
      builder.namespace(matcher.group(2)).key(matcher.group(1));
    } else if (context.isNamespaceSet()) {
      builder.namespace(context.getNamespace()).key(s);
    } else {
      builder.key(s);
    }
    Keys.SelfKey key = builder.build();
    key.metadata.ttl = (int) context.getKeyTtl();
    return key;
  }
}
