package org.atsign.cucumber.steps;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

import java.util.regex.Matcher;

import org.atsign.client.api.AtClient;
import org.atsign.client.util.KeyStringUtil;
import org.atsign.common.AtSign;
import org.atsign.common.KeyBuilders;
import org.atsign.common.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;

public class SharedAtKeySteps {

  private static final Logger LOGGER = LoggerFactory.getLogger(SharedAtKeySteps.class);

  private final AtClientContext context;

  public SharedAtKeySteps(AtClientContext context) {
    this.context = context;
  }

  // put

  @When("{ordinal} {atsign} AtClient.put for SharedKey {word} shared with {atsign} and value {string}")
  public void putAsOwner(Integer ordinal, AtSign clientAtSign, String name, AtSign sharedWith, String value)
      throws Exception {
    AtClient atClient = context.lookupAtClient(clientAtSign, ordinal);
    putKeyValue(atClient, clientAtSign, name, sharedWith, value);
  }

  @Then("{ordinal} {atsign} AtClient.put fails for SharedKey {word} shared with {atsign} and value {string}")
  public void putAsOwnerFails(Integer ordinal, AtSign clientAtSign, String name, AtSign sharedWith, String value)
      throws Exception {
    AtClient atClient = context.lookupAtClient(clientAtSign, ordinal);
    context.assertException(() -> putKeyValue(atClient, clientAtSign, name, sharedWith, value));
  }

  @When("{atsign} AtClient.put for SharedKey {word} shared with {atsign} and value {string}")
  public void putAsOwner(AtSign clientAtSign, String name, AtSign sharedWith, String value) throws Exception {
    AtClient atClient = context.lookupOrCreateAtClient(clientAtSign);
    putKeyValue(atClient, clientAtSign, name, sharedWith, value);
  }

  @Then("{atsign} AtClient.put fails for SharedKey {word} shared with {atsign} and value {string}")
  public void putAsOwnerFails(AtSign clientAtSign, String name, AtSign sharedWith, String value) throws Exception {
    AtClient atClient = context.lookupOrCreateAtClient(clientAtSign);
    context.assertException(() -> putKeyValue(atClient, clientAtSign, name, sharedWith, value));
  }

  @When("AtClient.put for SharedKey {word} shared with {atsign} and value {string}")
  public void putAsOwner(String name, AtSign sharedWith, String value) throws Exception {
    QualifiedAtSign currentQualifiedAtSign = context.getCurrentQualifiedAtSign();
    AtClient atClient = context.lookupAtClient(currentQualifiedAtSign);
    putKeyValue(atClient, currentQualifiedAtSign.getAtSign(), name, sharedWith, value);
  }

  @Then("AtClient.put for fails SharedKey {word} shared with {atsign} and value {string}")
  public void putAsOwnerFails(String name, AtSign sharedWith, String value) throws Exception {
    QualifiedAtSign currentQualifiedAtSign = context.getCurrentQualifiedAtSign();
    AtClient atClient = context.lookupAtClient(currentQualifiedAtSign);
    context.assertException(() -> putKeyValue(atClient, currentQualifiedAtSign.getAtSign(), name, sharedWith, value));
  }

  // delete

  @When("{ordinal} {atsign} AtClient.delete for SharedKey {word} shared with {atsign}")
  public void deleteAsOwner(Integer ordinal, AtSign clientAtSign, String name, AtSign sharedWith) throws Exception {
    AtClient atClient = context.lookupAtClient(clientAtSign, ordinal);
    deleteKeyValue(atClient, clientAtSign, name, sharedWith);
  }

  @Then("{ordinal} {atsign} AtClient.delete fails for SharedKey {word} shared with {atsign}")
  public void deleteAsOwnerFails(Integer ordinal, AtSign clientAtSign, String name, AtSign sharedWith)
      throws Exception {
    AtClient atClient = context.lookupAtClient(clientAtSign, ordinal);
    context.assertException(() -> deleteKeyValue(atClient, clientAtSign, name, sharedWith));
  }

  @When("{atsign} AtClient.delete for SharedKey {word} shared with {atsign}")
  public void deleteAsOwner(AtSign clientAtSign, String name, AtSign sharedWith) throws Exception {
    AtClient atClient = context.lookupOrCreateAtClient(clientAtSign);
    deleteKeyValue(atClient, clientAtSign, name, sharedWith);
  }

  @Then("{atsign} AtClient.delete fails for SharedKey {word} shared with {atsign}")
  public void deleteAsOwnerFails(AtSign clientAtSign, String name, AtSign sharedWith) throws Exception {
    AtClient atClient = context.lookupOrCreateAtClient(clientAtSign);
    context.assertException(() -> deleteKeyValue(atClient, clientAtSign, name, sharedWith));
  }

  @When("AtClient.delete for SharedKey {word} shared with {atsign}")
  public void deleteAsOwner(String name, AtSign sharedWith) throws Exception {
    QualifiedAtSign currentQualifiedAtSign = context.getCurrentQualifiedAtSign();
    AtClient atClient = context.lookupAtClient(currentQualifiedAtSign);
    deleteKeyValue(atClient, currentQualifiedAtSign.getAtSign(), name, sharedWith);
  }

  // get as owner

  @Then("{ordinal} {atsign} AtClient.get for SharedKey {word} shared with {atsign} returns value that matches {string}")
  public void assertGetAsOwner(Integer ordinal, AtSign clientAtSign, String name, AtSign sharedWith, String expected)
      throws Exception {
    AtClient atClient = context.lookupAtClient(clientAtSign, ordinal);
    String keyValue = getKeyValue(atClient, clientAtSign, name, sharedWith);
    assertThat(keyValue, equalTo(expected));
  }

  @Then("{ordinal} {atsign} AtClient.get fails for SharedKey {word} shared with {atsign}")
  public void assertGetExceptionAsOwner(Integer ordinal, AtSign clientAtSign, String name, AtSign sharedWith) {
    AtClient atClient = context.lookupAtClient(clientAtSign, ordinal);
    context.assertException(() -> getKeyValue(atClient, clientAtSign, name, sharedWith));
  }

  @Then("{atsign} AtClient.get for SharedKey {word} shared with {atsign} returns value that matches {string}")
  public void assertGetAsOwner(AtSign clientAtSign, String name, AtSign sharedWith, String expected) throws Exception {
    AtClient atClient = context.lookupOrCreateAtClient(clientAtSign);
    String keyValue = getKeyValue(atClient, clientAtSign, name, sharedWith);
    assertThat(keyValue, equalTo(expected));
  }

  @Then("{atsign} AtClient.get fails for SharedKey {word} shared with {atsign}")
  public void assertGetExceptionAsOwner(AtSign clientAtSign, String name, AtSign sharedWith) throws Exception {
    AtClient atClient = context.lookupOrCreateAtClient(clientAtSign);
    context.assertException(() -> getKeyValue(atClient, clientAtSign, name, sharedWith));
  }

  @Then("AtClient.get for SharedKey {word} shared with {atsign} returns value that matches {string}")
  public void assertGetAsOwner(String name, AtSign sharedWith, String expected) throws Exception {
    QualifiedAtSign currentQualifiedAtSign = context.getCurrentQualifiedAtSign();
    AtClient atClient = context.lookupAtClient(currentQualifiedAtSign);
    String keyValue = getKeyValue(atClient, currentQualifiedAtSign.getAtSign(), name, sharedWith);
    assertThat(keyValue, equalTo(expected));
  }

  @Then("AtClient.get fails for SharedKey {word} shared with {atsign}")
  public void assertGetExceptionAsOwner(String name, AtSign sharedWith) {
    QualifiedAtSign currentQualifiedAtSign = context.getCurrentQualifiedAtSign();
    AtClient atClient = context.lookupAtClient(currentQualifiedAtSign);
    context.assertException(() -> getKeyValue(atClient, currentQualifiedAtSign.getAtSign(), name, sharedWith));
  }

  // get as recipient

  @Then("{ordinal} {atsign} AtClient.get for SharedKey {word} shared by {atsign} returns value that matches {string}")
  public void assertGetAsRecipient(Integer ordinal, AtSign clientAtSign, String name, AtSign sharedBy, String expected)
      throws Exception {
    AtClient atClient = context.lookupAtClient(clientAtSign, ordinal);
    String keyValue = getKeyValue(atClient, sharedBy, name, clientAtSign);
    assertThat(keyValue, equalTo(expected));
  }

  @Then("{ordinal} {atsign} AtClient.get fails for SharedKey {word} shared by {atsign}}")
  public void assertGetSharedKeyResultForRecipientFails(Integer ordinal, AtSign clientAtSign, String name,
                                                        AtSign sharedBy)
      throws Exception {
    AtClient atClient = context.lookupAtClient(clientAtSign, ordinal);
    context.assertException(() -> getKeyValue(atClient, sharedBy, name, clientAtSign));
  }

  @Then("{atsign} AtClient.get for SharedKey {word} shared by {atsign} returns value that matches {string}")
  public void assertGetAsRecipient(AtSign clientAtSign, String name, AtSign sharedBy, String expected)
      throws Exception {
    AtClient atClient = context.lookupOrCreateAtClient(clientAtSign);
    String keyValue = getKeyValue(atClient, sharedBy, name, clientAtSign);
    assertThat(keyValue, equalTo(expected));
  }

  @Then("{atsign} AtClient.get fails for SharedKey {word} shared by {atsign}")
  public void assertGetAsRecipientFails(AtSign clientAtSign, String name, AtSign sharedBy) throws Exception {
    AtClient atClient = context.lookupOrCreateAtClient(clientAtSign);
    context.assertException(() -> getKeyValue(atClient, sharedBy, name, clientAtSign));
  }

  @Then("AtClient.get for SharedKey {word} shared by {atsign} returns value that matches {string}")
  public void assertGetAsRecipient(String name, AtSign sharedBy, String expected) throws Exception {
    QualifiedAtSign currentQualifiedAtSign = context.getCurrentQualifiedAtSign();
    AtClient atClient = context.lookupAtClient(currentQualifiedAtSign);
    String keyValue = getKeyValue(atClient, sharedBy, name, currentQualifiedAtSign.getAtSign());
    assertThat(keyValue, equalTo(expected));
  }

  @Then("AtClient.get fails for SharedKey {word} shared by {atsign}")
  public void assertGetAsRecipientFails(String name, AtSign sharedBy, String expected) throws Exception {
    QualifiedAtSign currentQualifiedAtSign = context.getCurrentQualifiedAtSign();
    AtClient atClient = context.lookupAtClient(currentQualifiedAtSign);
    context.assertException(() -> getKeyValue(atClient, sharedBy, name, currentQualifiedAtSign.getAtSign()));
  }

  private void putKeyValue(AtClient atClient, AtSign sharedBy, String name, AtSign sharedWith, String value)
      throws Exception {
    Keys.SharedKey key = createKey(sharedBy, name, sharedWith);
    atClient.put(key, value).get();
  }

  private String getKeyValue(AtClient atClient, AtSign sharedBy, String name, AtSign sharedWith) throws Exception {
    Keys.SharedKey key = createKey(sharedBy, name, sharedWith);
    return atClient.get(key).get();
  }

  private void deleteKeyValue(AtClient atClient, AtSign sharedBy, String name, AtSign sharedWith) throws Exception {
    Keys.SharedKey key = createKey(sharedBy, name, sharedWith);
    atClient.delete(key).get();
  }

  private Keys.SharedKey createKey(AtSign sharedBy, String s, AtSign sharedWith) {
    KeyBuilders.SharedKeyBuilder builder = new KeyBuilders.SharedKeyBuilder(sharedBy, sharedWith);
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
    Keys.SharedKey key = builder.build();
    key.metadata.ttl = (int) context.getKeyTtl();
    return key;
  }

}
