package org.atsign.cucumber.steps;

import static org.atsign.cucumber.steps.ParameterTypes.toBytes;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

import java.util.regex.Matcher;

import io.cucumber.datatable.DataTable;
import org.atsign.client.api.AtClient;
import org.atsign.client.api.AtSign;
import org.atsign.client.api.Keys;

import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.atsign.client.impl.exceptions.AtException;

public class SelfAtKeySteps {

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

  @When("{ordinal} {atsign} AtClient.put for SelfKey {word} and bytes")
  public void put(Integer ordinal, AtSign clientAtSign, String name, DataTable table) throws Exception {
    AtClient atClient = context.lookupAtClient(clientAtSign, ordinal);
    putKeyValue(atClient, clientAtSign, name, toBytes(table));
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

  @When("{atsign} AtClient.put for SelfKey {word} and bytes")
  public void put(AtSign clientAtSign, String name, DataTable table) throws Exception {
    AtClient atClient = context.lookupOrCreateAtClient(clientAtSign);
    putKeyValue(atClient, clientAtSign, name, toBytes(table));
  }

  @When("AtClient.put for SelfKey {word} and value {string}")
  public void put(String name, String value) throws Exception {
    QualifiedAtSign currentQualifiedAtSign = context.getCurrentQualifiedAtSign();
    AtClient atClient = context.lookupAtClient(currentQualifiedAtSign);
    putKeyValue(atClient, currentQualifiedAtSign.getAtSign(), name, value);
  }

  @When("AtClient.put for SelfKey {word} and bytes")
  public void put(String name, DataTable table) throws Exception {
    QualifiedAtSign currentQualifiedAtSign = context.getCurrentQualifiedAtSign();
    AtClient atClient = context.lookupAtClient(currentQualifiedAtSign);
    putKeyValue(atClient, currentQualifiedAtSign.getAtSign(), name, toBytes(table));
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

  @Then("{ordinal} {atsign} AtClient.getBinary for SelfKey {word} returns bytes that matches")
  public void assertGet(Integer ordinal, AtSign clientAtSign, String name, DataTable table) throws Exception {
    AtClient atClient = context.lookupAtClient(clientAtSign, ordinal);
    byte[] keyValue = getBinaryKeyValue(atClient, clientAtSign, name);
    assertThat(keyValue, equalTo(toBytes(table)));
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

  @Then("{atsign} AtClient.getBinary for SelfKey {word} returns bytes that matches")
  public void assertGet(AtSign clientAtSign, String name, DataTable table) throws Exception {
    AtClient atClient = context.lookupOrCreateAtClient(clientAtSign);
    byte[] keyValue = getBinaryKeyValue(atClient, clientAtSign, name);
    assertThat(keyValue, equalTo(toBytes(table)));
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

  @Then("AtClient.getBinary for SelfKey {word} returns bytes that matches")
  public void assertGet(String name, DataTable table) throws Exception {
    QualifiedAtSign currentQualifiedAtSign = context.getCurrentQualifiedAtSign();
    AtClient atClient = context.lookupAtClient(currentQualifiedAtSign);
    byte[] keyValue = getBinaryKeyValue(atClient, currentQualifiedAtSign.getAtSign(), name);
    assertThat(keyValue, equalTo(toBytes(table)));
  }

  @Then("AtClient.get fails for SelfKey {word}")
  public void assertGetFails(String name) throws Exception {
    QualifiedAtSign currentQualifiedAtSign = context.getCurrentQualifiedAtSign();
    AtClient atClient = context.lookupAtClient(currentQualifiedAtSign);
    context.assertException(() -> getKeyValue(atClient, currentQualifiedAtSign.getAtSign(), name));
  }

  private void putKeyValue(AtClient atClient, AtSign atSign, String name, String value)
      throws AtException {
    Keys.SelfKey key = createKey(atSign, name);
    atClient.put(key, value);
  }

  private void putKeyValue(AtClient atClient, AtSign atSign, String name, byte[] value)
      throws AtException {
    Keys.SelfKey key = createKey(atSign, name);
    atClient.put(key, value);
  }

  private String getKeyValue(AtClient atClient, AtSign atSign, String name) throws Exception {
    Keys.SelfKey key = createKey(atSign, name);
    return atClient.get(key);
  }

  private byte[] getBinaryKeyValue(AtClient atClient, AtSign atSign, String name) throws Exception {
    Keys.SelfKey key = createKey(atSign, name);
    return atClient.getBinary(key);
  }

  private void deleteKeyValue(AtClient atClient, AtSign atSign, String name) throws Exception {
    Keys.SelfKey key = createKey(atSign, name);
    atClient.delete(key);
  }

  private Keys.SelfKey createKey(AtSign owner, String s) {
    Keys.SelfKeyBuilder builder = Keys.selfKeyBuilder()
        .sharedBy(owner)
        .ttl(context.getKeyTtl());
    Matcher matcher = Keys.createNamespaceQualifiedKeyNameMatcher(s);
    if (matcher.matches()) {
      if (context.isNamespaceSet()) {
        throw new IllegalArgumentException("context has namespace set, intention is ambiguous");
      }
      builder.namespace(matcher.group(2)).name(matcher.group(1));
    } else if (context.isNamespaceSet()) {
      builder.namespace(context.getNamespace()).name(s);
    } else {
      builder.name(s);
    }
    return builder.build();
  }
}
