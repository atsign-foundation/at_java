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

public class PublicAtKeySteps {

  private final AtClientContext context;

  public PublicAtKeySteps(AtClientContext context) {
    this.context = context;
  }

  // put

  @When("{ordinal} {atsign} AtClient.put for PublicKey {word} and value {string}")
  public void put(Integer ordinal, AtSign clientAtSign, String name, String value) throws Exception {
    AtClient atClient = context.lookupAtClient(clientAtSign, ordinal);
    putKeyValue(atClient, clientAtSign, name, value);
  }

  @When("{ordinal} {atsign} AtClient.put for PublicKey {word} and bytes")
  public void put(Integer ordinal, AtSign clientAtSign, String name, DataTable table) throws Exception {
    AtClient atClient = context.lookupAtClient(clientAtSign, ordinal);
    putKeyValue(atClient, clientAtSign, name, toBytes(table));
  }

  @When("{ordinal} {atsign} AtClient.put fails for PublicKey {word} and value {string}")
  public void putFails(Integer ordinal, AtSign clientAtSign, String name, String value) throws Exception {
    AtClient atClient = context.lookupAtClient(clientAtSign, ordinal);
    context.assertException(() -> putKeyValue(atClient, clientAtSign, name, value));
  }

  @When("{atsign} AtClient.put for PublicKey {word} and value {string}")
  public void put(AtSign clientAtSign, String name, String value) throws Exception {
    AtClient atClient = context.lookupOrCreateAtClient(clientAtSign);
    putKeyValue(atClient, clientAtSign, name, value);
  }

  @When("{atsign} AtClient.put for PublicKey {word} and bytes")
  public void put(AtSign clientAtSign, String name, DataTable table) throws Exception {
    AtClient atClient = context.lookupOrCreateAtClient(clientAtSign);
    putKeyValue(atClient, clientAtSign, name, toBytes(table));
  }

  @When("{atsign} AtClient.put fails for PublicKey {word} and value {string}")
  public void putFails(AtSign clientAtSign, String name, String value) throws Exception {
    AtClient atClient = context.lookupOrCreateAtClient(clientAtSign);
    context.assertException(() -> putKeyValue(atClient, clientAtSign, name, value));
  }

  @When("AtClient.put for PublicKey {word} and value {string}")
  public void put(String name, String value) throws Exception {
    QualifiedAtSign currentQualifiedAtSign = context.getCurrentQualifiedAtSign();
    AtClient atClient = context.lookupAtClient(currentQualifiedAtSign);
    putKeyValue(atClient, currentQualifiedAtSign.getAtSign(), name, value);
  }

  @When("AtClient.put for PublicKey {word} and bytes")
  public void put(String name, DataTable table) throws Exception {
    QualifiedAtSign currentQualifiedAtSign = context.getCurrentQualifiedAtSign();
    AtClient atClient = context.lookupAtClient(currentQualifiedAtSign);
    putKeyValue(atClient, currentQualifiedAtSign.getAtSign(), name, toBytes(table));
  }

  @When("AtClient.put fails for PublicKey {word} and value {string}")
  public void putFails(String name, String value) throws Exception {
    QualifiedAtSign currentQualifiedAtSign = context.getCurrentQualifiedAtSign();
    AtClient atClient = context.lookupAtClient(currentQualifiedAtSign);
    context.assertException(() -> putKeyValue(atClient, currentQualifiedAtSign.getAtSign(), name, value));
  }

  // delete

  @When("{ordinal} {atsign} AtClient.delete for PublicKey {word}")
  public void delete(Integer ordinal, AtSign clientAtSign, String name) throws Exception {
    AtClient atClient = context.lookupAtClient(clientAtSign, ordinal);
    deleteKeyValue(atClient, clientAtSign, name);
  }

  @Then("{ordinal} {atsign} AtClient.delete fails for PublicKey {word}")
  public void deleteFails(Integer ordinal, AtSign clientAtSign, String name) throws Exception {
    AtClient atClient = context.lookupAtClient(clientAtSign, ordinal);
    context.assertException(() -> deleteKeyValue(atClient, clientAtSign, name));
  }

  @When("{atsign} AtClient.delete for PublicKey {word}")
  public void delete(AtSign clientAtSign, String name) throws Exception {
    AtClient atClient = context.lookupOrCreateAtClient(clientAtSign);
    deleteKeyValue(atClient, clientAtSign, name);
  }

  @Then("{atsign} AtClient.delete fails for PublicKey {word}")
  public void deleteFails(AtSign clientAtSign, String name) throws Exception {
    AtClient atClient = context.lookupOrCreateAtClient(clientAtSign);
    context.assertException(() -> deleteKeyValue(atClient, clientAtSign, name));
  }

  @When("AtClient.delete for PublicKey {word}")
  public void delete(String name) throws Exception {
    QualifiedAtSign currentQualifiedAtSign = context.getCurrentQualifiedAtSign();
    AtClient atClient = context.lookupAtClient(currentQualifiedAtSign);
    deleteKeyValue(atClient, currentQualifiedAtSign.getAtSign(), name);
  }

  @When("AtClient.delete fails for PublicKey {word}")
  public void deleteFails(String name) throws Exception {
    QualifiedAtSign currentQualifiedAtSign = context.getCurrentQualifiedAtSign();
    AtClient atClient = context.lookupAtClient(currentQualifiedAtSign);
    context.assertException(() -> deleteKeyValue(atClient, currentQualifiedAtSign.getAtSign(), name));
  }

  // get

  @Then("{ordinal} {atsign} AtClient.get for PublicKey {word} returns value that matches {string}")
  public void getAsOwner(Integer ordinal, AtSign clientAtSign, String name, String expected) throws Exception {
    AtClient atClient = context.lookupAtClient(clientAtSign, ordinal);
    String keyValue = getKeyValue(atClient, clientAtSign, name);
    assertThat(keyValue, equalTo(expected));
  }

  @Then("{ordinal} {atsign} AtClient.getBinary for PublicKey {word} returns bytes that matches")
  public void getAsOwner(Integer ordinal, AtSign clientAtSign, String name, DataTable expected) throws Exception {
    AtClient atClient = context.lookupAtClient(clientAtSign, ordinal);
    byte[] keyValue = getBinaryKeyValue(atClient, clientAtSign, name);
    assertThat(keyValue, equalTo(toBytes(expected)));
  }

  @Then("{ordinal} {atsign} AtClient.get fails for PublicKey {word}")
  public void getAsOwnerFails(Integer ordinal, AtSign clientAtSign, String name) throws Exception {
    AtClient atClient = context.lookupAtClient(clientAtSign, ordinal);
    context.assertException(() -> getKeyValue(atClient, clientAtSign, name));
  }

  @Then("{atsign} AtClient.get for PublicKey {word} returns value that matches {string}")
  public void getAsOwner(AtSign clientAtSign, String name, String expected) throws Exception {
    AtClient atClient = context.lookupOrCreateAtClient(clientAtSign);
    String keyValue = getKeyValue(atClient, clientAtSign, name);
    assertThat(keyValue, equalTo(expected));
  }

  @Then("{atsign} AtClient.getBinary for PublicKey {word} returns bytes that matches")
  public void getAsOwner(AtSign clientAtSign, String name, DataTable table) throws Exception {
    AtClient atClient = context.lookupOrCreateAtClient(clientAtSign);
    byte[] keyValue = getBinaryKeyValue(atClient, clientAtSign, name);
    assertThat(keyValue, equalTo(toBytes(table)));
  }

  @Then("{atsign} AtClient.get fails for PublicKey {word}")
  public void getAsOwnerFails(AtSign clientAtSign, String name) throws Exception {
    AtClient atClient = context.lookupOrCreateAtClient(clientAtSign);
    context.assertException(() -> getKeyValue(atClient, clientAtSign, name));
  }

  @Then("AtClient.get for PublicKey {word} returns value that matches {string}")
  public void getAsOwner(String name, String expected) throws Exception {
    QualifiedAtSign currentQualifiedAtSign = context.getCurrentQualifiedAtSign();
    AtClient atClient = context.lookupAtClient(currentQualifiedAtSign);
    String keyValue = getKeyValue(atClient, currentQualifiedAtSign.getAtSign(), name);
    assertThat(keyValue, equalTo(expected));
  }

  @Then("AtClient.getBinary for PublicKey {word} returns bytes that matches")
  public void getAsOwner(String name, DataTable expected) throws Exception {
    QualifiedAtSign currentQualifiedAtSign = context.getCurrentQualifiedAtSign();
    AtClient atClient = context.lookupAtClient(currentQualifiedAtSign);
    byte[] keyValue = getBinaryKeyValue(atClient, currentQualifiedAtSign.getAtSign(), name);
    assertThat(keyValue, equalTo(toBytes(expected)));
  }

  @Then("AtClient.get fails for PublicKey {word}")
  public void getAsOwnerFails(String name) throws Exception {
    QualifiedAtSign currentQualifiedAtSign = context.getCurrentQualifiedAtSign();
    AtClient atClient = context.lookupAtClient(currentQualifiedAtSign);
    context.assertException(() -> getKeyValue(atClient, currentQualifiedAtSign.getAtSign(), name));
  }

  @Then("{ordinal} {atsign} AtClient.get for PublicKey {word} shared by {atsign} returns value that matches {string}")
  public void getAsNonOwner(Integer ordinal, AtSign clientAtSign, String name, AtSign sharedBy, String expected)
      throws Exception {
    AtClient atClient = context.lookupAtClient(clientAtSign, ordinal);
    String keyValue = getKeyValue(atClient, sharedBy, name);
    assertThat(keyValue, equalTo(expected));
  }

  @Then("{ordinal} {atsign} AtClient.getBinary for PublicKey {word} shared by {atsign} returns bytes that matches")
  public void getAsNonOwner(Integer ordinal, AtSign clientAtSign, String name, AtSign sharedBy, DataTable table)
      throws Exception {
    AtClient atClient = context.lookupAtClient(clientAtSign, ordinal);
    byte[] keyValue = getBinaryKeyValue(atClient, sharedBy, name);
    assertThat(keyValue, equalTo(toBytes(table)));
  }

  @Then("{ordinal} {atsign} AtClient.get fails for PublicKey shared by {atsign}")
  public void getAsNonOwnerFails(Integer ordinal, AtSign clientAtSign, String name, AtSign sharedBy) throws Exception {
    AtClient atClient = context.lookupAtClient(clientAtSign, ordinal);
    context.assertException(() -> getKeyValue(atClient, sharedBy, name));
  }

  @Then("{atsign} AtClient.get for PublicKey {word} shared by {atsign} returns value that matches {string}")
  public void getAsNonOwner(AtSign clientAtSign, String name, AtSign sharedBy, String expected) throws Exception {
    AtClient atClient = context.lookupOrCreateAtClient(clientAtSign);
    String keyValue = getKeyValue(atClient, sharedBy, name);
    assertThat(keyValue, equalTo(expected));
  }

  @Then("{atsign} AtClient.getBinary for PublicKey {word} shared by {atsign} returns bytes that matches")
  public void getAsNonOwner(AtSign clientAtSign, String name, AtSign sharedBy, DataTable table) throws Exception {
    AtClient atClient = context.lookupOrCreateAtClient(clientAtSign);
    byte[] keyValue = getBinaryKeyValue(atClient, sharedBy, name);
    assertThat(keyValue, equalTo(toBytes(table)));
  }

  @Then("{atsign} AtClient.get fails for PublicKey {word} shared by {atsign}")
  public void getAsNonOwnerFails(AtSign clientAtSign, String name, AtSign sharedBy) throws Exception {
    AtClient atClient = context.lookupOrCreateAtClient(clientAtSign);
    context.assertException(() -> getKeyValue(atClient, sharedBy, name));
  }

  @Then("AtClient.get for PublicKey {word} shared by {atsign} returns value that matches {string}")
  public void getAsNonOwner(String name, AtSign sharedBy, String expected) throws Exception {
    QualifiedAtSign currentQualifiedAtSign = context.getCurrentQualifiedAtSign();
    AtClient atClient = context.lookupAtClient(currentQualifiedAtSign);
    String keyValue = getKeyValue(atClient, sharedBy, name);
    assertThat(keyValue, equalTo(expected));
  }

  @Then("AtClient.getBinary for PublicKey {word} shared by {atsign} returns bytes that matches")
  public void getAsNonOwner(String name, AtSign sharedBy, DataTable table) throws Exception {
    QualifiedAtSign currentQualifiedAtSign = context.getCurrentQualifiedAtSign();
    AtClient atClient = context.lookupAtClient(currentQualifiedAtSign);
    byte[] keyValue = getBinaryKeyValue(atClient, sharedBy, name);
    assertThat(keyValue, equalTo(toBytes(table)));
  }

  @Then("AtClient.get fails for PublicKey {word} shared by {atsign}")
  public void getAsNonOwnerFails(String name, AtSign sharedBy) throws Exception {
    QualifiedAtSign currentQualifiedAtSign = context.getCurrentQualifiedAtSign();
    AtClient atClient = context.lookupAtClient(currentQualifiedAtSign);
    context.assertException(() -> getKeyValue(atClient, sharedBy, name));
  }

  private void putKeyValue(AtClient atClient, AtSign sharedBy, String name, String value)
      throws AtException {
    Keys.PublicKey key = toKey(sharedBy, name);
    atClient.put(key, value);
  }

  private void putKeyValue(AtClient atClient, AtSign sharedBy, String name, byte[] value)
      throws AtException {
    Keys.PublicKey key = toKey(sharedBy, name);
    atClient.put(key, value);
  }

  private String getKeyValue(AtClient atClient, AtSign sharedBy, String name) throws Exception {
    Keys.PublicKey key = toKey(sharedBy, name);
    return atClient.get(key);
  }

  private byte[] getBinaryKeyValue(AtClient atClient, AtSign sharedBy, String name) throws Exception {
    Keys.PublicKey key = toKey(sharedBy, name);
    return atClient.getBinary(key);
  }

  private void deleteKeyValue(AtClient atClient, AtSign owner, String name) throws Exception {
    Keys.PublicKey key = toKey(owner, name);
    atClient.delete(key);
  }

  private Keys.PublicKey toKey(AtSign sharedBy, String s) {
    Keys.PublicKeyBuilder builder = Keys.publicKeyBuilder()
        .sharedBy(sharedBy)
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
