package org.atsign.cucumber.steps;

import static java.util.Arrays.asList;
import static org.atsign.cucumber.helpers.Helpers.assertContains;
import static org.atsign.cucumber.helpers.Helpers.toCanonicalKey;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

import java.util.*;
import java.util.stream.Collectors;

import org.atsign.client.api.AtClient;
import org.atsign.client.api.AtSign;
import org.atsign.client.api.Keys;

import io.cucumber.datatable.DataTable;
import io.cucumber.datatable.DataTableFormatter;
import io.cucumber.java.en.And;
import io.cucumber.java.en.Then;

public class GetAtKeysSteps {

  public static final List<String> HEADINGS = asList(
                                                     "Key",
                                                     "Name",
                                                     "Namespace",
                                                     "Shared By",
                                                     "Shared With");

  public static final List<String> HEADINGS_INCLUDING_VALUE = asList(
                                                                     "Key",
                                                                     "Name",
                                                                     "Namespace",
                                                                     "Shared By",
                                                                     "Shared With",
                                                                     "Value");

  public static final List<String> HEADINGS_INCLUDING_METADATA = asList(
                                                                        "Key",
                                                                        "Name",
                                                                        "Namespace",
                                                                        "Shared By",
                                                                        "Shared With",
                                                                        "Ttl",
                                                                        "Ttb",
                                                                        "Ttr",
                                                                        "Ccd",
                                                                        "Created By",
                                                                        "Updated By",
                                                                        "Available At",
                                                                        "Expires At",
                                                                        "Refresh At",
                                                                        "Created At",
                                                                        "Updated At",
                                                                        "Status",
                                                                        "Version",
                                                                        "Data Signature",
                                                                        "Shared Key Status",
                                                                        "Is Public",
                                                                        "Is Encrypted",
                                                                        "Is Hidden",
                                                                        "Namespace Aware",
                                                                        "Is Binary",
                                                                        "Is Cached",
                                                                        "Shared Key Enc",
                                                                        "Pub Key CS",
                                                                        "Encoding");

  private final AtClientContext context;

  public GetAtKeysSteps(AtClientContext context) {
    this.context = context;
  }

  // matches

  @Then("{ordinal} {atsign} AtClient.getAtKeys for {string} matches")
  public void assertGetAtKeysMatches(Integer ordinal, AtSign clientAtSign, String regex, DataTable expected)
      throws Exception {
    AtClient atClient = context.lookupAtClient(clientAtSign, ordinal);
    assertGetAtKeysMatches(regex, expected, atClient);
  }

  @Then("{atsign} AtClient.getAtKeys for {string} matches")
  public void assertGetAtKeysMatches(AtSign clientAtSign, String regex, DataTable expected) throws Exception {
    AtClient atClient = context.lookupOrCreateAtClient(clientAtSign);
    assertGetAtKeysMatches(regex, expected, atClient);
  }

  @Then("AtClient.getAtKeys for {string} matches")
  public void assertGetAtKeysMatches(String regex, DataTable expected) throws Exception {
    AtClient atClient = context.lookupAtClient(context.getCurrentQualifiedAtSign());
    assertGetAtKeysMatches(regex, expected, atClient);
  }

  // contains

  @Then("{ordinal} {atsign} AtClient.getAtKeys for {string} contains")
  public void assertGetAtKeysContains(Integer ordinal, AtSign clientAtSign, String regex, DataTable expected)
      throws Exception {
    AtClient atClient = context.lookupAtClient(clientAtSign, ordinal);
    assertGetAtKeysContains(atClient, regex, expected);
  }

  @Then("{atsign} AtClient.getAtKeys for {string} contains")
  public void assertGetAtKeysContains(AtSign clientAtSign, String regex, DataTable expected) throws Exception {
    AtClient atClient = context.lookupOrCreateAtClient(clientAtSign);
    assertGetAtKeysContains(atClient, regex, expected);
  }

  @Then("AtClient.getAtKeys for {string} contains")
  public void assertGetAtKeysContains(String regex, DataTable expected) throws Exception {
    AtClient atClient = context.lookupAtClient(context.getCurrentQualifiedAtSign());
    assertGetAtKeysContains(atClient, regex, expected);
  }

  // NOT contains

  @Then("{ordinal} {atsign} AtClient.getAtKeys for {string} does NOT contain")
  public void assertGetAtKeysNotContains(Integer ordinal, AtSign clientAtSign, String regex, DataTable notExpected)
      throws Exception {
    AtClient atClient = context.lookupAtClient(clientAtSign, ordinal);
    assertGetAtKeysNotContains(regex, notExpected, atClient);
  }

  @Then("{atsign} AtClient.getAtKeys for {string} does NOT contain")
  public void assertGetAtKeysNotContains(AtSign clientAtSign, String regex, DataTable notExpected) throws Exception {
    AtClient atClient = context.lookupOrCreateAtClient(clientAtSign);
    assertGetAtKeysNotContains(regex, notExpected, atClient);
  }

  @Then("AtClient.getAtKeys for {string} does NOT contain")
  public void assertGetAtKeysNotContains(String regex, DataTable notExpected) throws Exception {
    AtClient atClient = context.lookupAtClient(context.getCurrentQualifiedAtSign());
    assertGetAtKeysNotContains(regex, notExpected, atClient);
  }

  private void assertGetAtKeysMatches(String regex, DataTable expected, AtClient atClient) throws Exception {
    List<Map<String, String>> actual = getAtKeysAsListOfMaps(atClient, regex);
    assertContains(actual, expected.asMaps(), true);
  }

  private void assertGetAtKeysContains(AtClient atClient, String regex, DataTable table) throws Exception {
    List<Map<String, String>> expected = new ArrayList<>();
    if (table.width() == 1) {
      for (String key : table.asList()) {
        if (key.equalsIgnoreCase("key")) {
          // this is a heading ignore
        } else {
          expected.add(Collections.singletonMap("Key", key));
        }
      }
    } else {
      assertThat(table.height(), greaterThan(1));
      expected.addAll(table.asMaps());
    }
    try {
      assertContains(getAtKeysAsListOfMaps(atClient, regex), expected, false);
    } catch (Exception | Error e) {
      dumpKeys(atClient, HEADINGS_INCLUDING_METADATA, true);
      throw e;
    }
  }

  private static void assertGetAtKeysNotContains(String regex, DataTable table, AtClient atClient) throws Exception {
    assertThat("expect single column datatable of keys (no heading)", table.width(), equalTo(1));
    Set<String> actual = atClient.getAtKeys(regex, false).get().stream()
        .map(Keys.AtKey::toString)
        .collect(Collectors.toSet());

    Set<String> intersection = new HashSet<>(table.asList());
    intersection.retainAll(actual);

    assertThat("contains " + intersection, intersection, empty());
  }

  @And("dump keys")
  public void dumpKeys() throws Exception {
    dumpKeys(context.lookupAtClient(context.getCurrentQualifiedAtSign()), HEADINGS, false);
  }

  @And("dump keys with metadata")
  public void dumpKeysWithMetaData() throws Exception {
    dumpKeys(context.lookupAtClient(context.getCurrentQualifiedAtSign()), HEADINGS_INCLUDING_METADATA, true);
  }

  @And("dump keys with values")
  public void dumpKeysWithValues() throws Exception {
    dumpKeys(context.lookupAtClient(context.getCurrentQualifiedAtSign()), HEADINGS_INCLUDING_VALUE, true);
  }

  @And("{atsign} dump keys with values")
  public void dumpKeysWithValues(AtSign clientAtSign) throws Exception {
    dumpKeys(context.lookupOnlyAtClient(clientAtSign), HEADINGS_INCLUDING_VALUE, true);
  }

  private void dumpKeys(AtClient atClient, List<String> headings, boolean fetchMetaData) throws Exception {
    List<List<String>> raw = new ArrayList<>();
    raw.add(headings);
    boolean lookupValue = headings.contains("Value");
    atClient.getAtKeys(".*", fetchMetaData).get().stream()
        .forEach(k -> raw.add(toDataTableRow(raw.get(0), k, lookupValue ? lookupStringValue(atClient, k) : null)));
    DataTableFormatter.builder()
        .prefixRow("  ")
        .escapeDelimiters(true)
        .build()
        .formatTo(DataTable.create(raw), System.out);
  }

  private String lookupStringValue(AtClient atClient, Keys.AtKey key) {
    try {
      if (key instanceof Keys.SharedKey) {
        return atClient.get((Keys.SharedKey) key).get();
      } else if (key instanceof Keys.PublicKey) {
        return atClient.get((Keys.PublicKey) key).get();
      } else if (key instanceof Keys.SelfKey) {
        return atClient.get((Keys.SelfKey) key).get();
      } else {
        return key.getClass().getSimpleName();
      }
    } catch (Exception e) {
      return e.getMessage();
    }
  }

  private List<String> toDataTableRow(List<String> headings, Keys.AtKey k, String value) {
    ArrayList<String> row = new ArrayList<>();
    for (String heading : headings) {
      switch (toCanonicalKey(heading)) {
        case "key":
          row.add(k.toString());
          break;
        case "value":
          row.add(value);
          break;
        case "name":
          row.add(k.nameWithoutNamespace());
          break;
        case "namespace":
          row.add(k.namespace());
          break;
        case "sharedby":
          row.add(withoutPrefix(k.sharedBy()));
          break;
        case "sharedwith":
          row.add(withoutPrefix(k.sharedWith()));
          break;
        case "ttl":
          row.add(toString(k.metadata().ttl()));
          break;
        case "ttb":
          row.add(toString(k.metadata().ttb()));
          break;
        case "ttr":
          row.add(toString(k.metadata().ttb()));
          break;
        case "ccd":
          row.add(toString(k.metadata().ccd()));
          break;
        case "createdby":
          row.add(toString(k.metadata().createdBy()));
          break;
        case "updatedby":
          row.add(toString(k.metadata().updatedBy()));
          break;
        case "availableat":
          row.add(toString(k.metadata().availableAt()));
          break;
        case "expiresat":
          row.add(toString(k.metadata().expiresAt()));
          break;
        case "refreshat":
          row.add(toString(k.metadata().refreshAt()));
          break;
        case "createdat":
          row.add(toString(k.metadata().createdAt()));
          break;
        case "updatedat":
          row.add(toString(k.metadata().updatedAt()));
          break;
        case "status":
          row.add(toString(k.metadata().status()));
          break;
        case "version":
          row.add(toString(k.metadata().version()));
          break;
        case "datasignature":
          row.add(toString(k.metadata().dataSignature()));
          break;
        case "sharedkeystatus":
          row.add(toString(k.metadata().sharedKeyStatus()));
          break;
        case "ispublic":
          row.add(toString(k.metadata().isPublic()));
          break;
        case "isencrypted":
          row.add(toString(k.metadata().isEncrypted()));
          break;
        case "ishidden":
          row.add(toString(k.metadata().isHidden()));
          break;
        case "namespaceaware":
          row.add(toString(k.metadata().namespaceAware()));
          break;
        case "isbinary":
          row.add(toString(k.metadata().isBinary()));
          break;
        case "iscached":
          row.add(toString(k.metadata().isCached()));
          break;
        case "sharedkeyenc":
          row.add(toString(k.metadata().sharedKeyEnc()));
          break;
        case "pubkeycs":
          row.add(toString(k.metadata().pubKeyCS()));
          break;
        case "encoding":
          row.add(toString(k.metadata().encoding()));
          break;
        default:
          throw new IllegalArgumentException(heading + " not recognised as a key or key metadata field");
      }
    }

    return row;
  }

  private List<Map<String, String>> getAtKeysAsListOfMaps(AtClient client, String regex) throws Exception {
    List<Map<String, String>> actual = new ArrayList<>();
    for (Keys.AtKey key : client.getAtKeys(regex, true).get()) {
      List<String> values = toDataTableRow(HEADINGS_INCLUDING_METADATA, key, null);
      Map<String, String> map = new HashMap<>();
      for (int i = 0; i < HEADINGS_INCLUDING_METADATA.size(); i++) {
        map.put(HEADINGS_INCLUDING_METADATA.get(i), values.get(i));
      }
      actual.add(map);
    }
    return actual;
  }

  private static String withoutPrefix(AtSign atSign) {
    return atSign != null ? atSign.withoutPrefix() : null;
  }

  private static String toString(Object o) {
    return o != null ? o.toString() : null;
  }

}
