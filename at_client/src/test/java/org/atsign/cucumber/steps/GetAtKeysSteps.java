package org.atsign.cucumber.steps;

import io.cucumber.datatable.DataTable;
import io.cucumber.datatable.DataTableFormatter;
import io.cucumber.java.en.And;
import io.cucumber.java.en.Then;
import org.atsign.common.AtException;
import org.atsign.common.AtSign;
import org.atsign.common.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

import static java.util.Arrays.asList;
import static org.atsign.cucumber.helpers.Helpers.assertContains;
import static org.atsign.cucumber.helpers.Helpers.toCanonicalKey;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

public class GetAtKeysSteps {

  private static final Logger LOGGER = LoggerFactory.getLogger(GetAtKeysSteps.class);

  public static final List<String> HEADINGS = asList(
      "Key",
      "Name",
      "Namespace",
      "Shared By",
      "Shared With"
  );

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

  @Then("{atsign} AtClient.getAtKeys for {string} matches")
  public void assertGetAtKeysMatches(AtSign atSign, String regex, DataTable table) throws Exception {
    assertContains(getAtKeysAsListOfMaps(atSign, regex), table.asMaps(), true);
  }

  @Then("AtClient.getAtKeys for {string} matches")
  public void assertGetAtKeysMatches(String regex, DataTable table) throws Exception {
    assertGetAtKeysMatches(context.getCurrentAtSign(), regex, table);
  }

  @Then("{atsign} AtClient.getAtKeys for {string} contains")
  public void assertGetAtKeysContains(AtSign atSign, String regex, DataTable table) throws Exception {
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
      assertContains(getAtKeysAsListOfMaps(atSign, regex), expected, false);
    } catch (Exception | Error e) {
      dumpKeys(atSign, HEADINGS_INCLUDING_METADATA, true);
      throw e;
    }
  }

  @Then("AtClient.getAtKeys for {string} contains")
  public void assertGetAtKeysContains(String regex, DataTable table) throws Exception {
    assertGetAtKeysContains(context.getCurrentAtSign(), regex, table);
  }

  @Then("{atsign} AtClient.getAtKeys for {string} does NOT contain")
  public void assertGetAtKeysNotContains(AtSign atSign, String regex, DataTable table) throws Exception {
    assertThat("expect single column datatable of keys (no heading)", table.width(), equalTo(1));
    Set<String> actual = context.getAtClient(atSign).getAtKeys(regex, false).get().stream()
        .map(key -> key.toString())
        .collect(Collectors.toSet());

    Set<String> intersection = new HashSet<>(table.asList());
    intersection.retainAll(actual);

    assertThat("contains " + intersection, intersection, empty());
  }

  @Then("AtClient.getAtKeys for {string} does NOT contain")
  public void assertGetAtKeysNotContains(String regex, DataTable table) throws Exception {
    assertGetAtKeysNotContains(context.getCurrentAtSign(), regex, table);
  }

  @And("dump keys")
  public void dumpKeys() throws Exception {
    dumpKeys(context.getCurrentAtSign(), HEADINGS, false);
  }

  @And("dump keys with metadata")
  public void dumpKeysWithMetaData() throws Exception {
    dumpKeys(context.getCurrentAtSign(), HEADINGS_INCLUDING_METADATA, true);
  }

  private void dumpKeys(AtSign atSign, List<String> headings, boolean fetchMetaData) throws InterruptedException, ExecutionException, AtException, IOException {
    List<List<String>> raw = new ArrayList<>();
    raw.add(headings);
    context.getAtClient(atSign).getAtKeys(".*", fetchMetaData).get().stream()
        .forEach(k -> raw.add(toDataTableRow(raw.get(0), k)));
    DataTableFormatter.builder()
        .prefixRow("  ")
        .escapeDelimiters(true)
        .build()
        .formatTo(DataTable.create(raw), System.out);
  }

  private List<String> toDataTableRow(List<String> headings, Keys.AtKey k) {
    ArrayList<String> row = new ArrayList<>();
    for (String heading : headings) {
      switch (toCanonicalKey(heading)) {
        case "key":
          row.add(k.toString());
          break;
        case "name":
          row.add(k.name);
          break;
        case "namespace":
          row.add(k.getNamespace());
          break;
        case "sharedby":
          row.add(k.sharedBy != null ? k.sharedBy.withoutPrefix() : null);
          break;
        case "sharedwith":
          row.add(k.sharedWith != null ? k.sharedWith.withoutPrefix() : null);
          break;
        case "ttl":
          row.add(k.metadata != null ? String.valueOf(k.metadata.ttl) : null);
          break;
        case "ttb":
          row.add(k.metadata != null ? String.valueOf(k.metadata.ttb) : null);
          break;
        case "ttr":
          row.add(k.metadata != null ? String.valueOf(k.metadata.ttr) : null);
          break;
        case "ccd":
          row.add(k.metadata != null ? String.valueOf(k.metadata.ccd) : null);
          break;
        case "createdby":
          row.add(k.metadata != null ? k.metadata.createdBy : null);
          break;
        case "updatedby":
          row.add(k.metadata != null ? k.metadata.updatedBy : null);
          break;
        case "availableat":
          row.add(k.metadata != null ? String.valueOf(k.metadata.availableAt) : null);
          break;
        case "expiresat":
          row.add(k.metadata != null ? String.valueOf(k.metadata.expiresAt) : null);
          break;
        case "refreshat":
          row.add(k.metadata != null ? String.valueOf(k.metadata.refreshAt) : null);
          break;
        case "createdat":
          row.add(k.metadata != null ? String.valueOf(k.metadata.createdAt) : null);
          break;
        case "updatedat":
          row.add(k.metadata != null ? String.valueOf(k.metadata.updatedAt) : null);
          break;
        case "status":
          row.add(k.metadata != null ? k.metadata.status : null);
          break;
        case "version":
          row.add(k.metadata != null ? String.valueOf(k.metadata.version) : null);
          break;
        case "datasignature":
          row.add(k.metadata != null ? k.metadata.dataSignature : null);
          break;
        case "sharedkeystatus":
          row.add(k.metadata != null ? k.metadata.sharedKeyStatus : null);
          break;
        case "ispublic":
          row.add(k.metadata != null ? String.valueOf(k.metadata.isPublic) : null);
          break;
        case "isencrypted":
          row.add(k.metadata != null ? String.valueOf(k.metadata.isEncrypted) : null);
          break;
        case "ishidden":
          row.add(k.metadata != null ? String.valueOf(k.metadata.isHidden) : null);
          break;
        case "namespaceaware":
          row.add(k.metadata != null ? String.valueOf(k.metadata.namespaceAware) : null);
          break;
        case "isbinary":
          row.add(k.metadata != null ? String.valueOf(k.metadata.isBinary) : null);
          break;
        case "iscached":
          row.add(k.metadata != null ? String.valueOf(k.metadata.isCached) : null);
          break;
        case "sharedkeyenc":
          row.add(k.metadata != null ? k.metadata.sharedKeyEnc : null);
          break;
        case "pubkeycs":
          row.add(k.metadata != null ? k.metadata.pubKeyCS : null);
          break;
        case "encoding":
          row.add(k.metadata != null ? k.metadata.encoding : null);
          break;
        default:
          throw new IllegalArgumentException(heading + " not recognised as a key or key metadata field");
      }
    }

    return row;
  }

  private List<Map<String, String>> getAtKeysAsListOfMaps(AtSign atSign, String regex) throws Exception {
    List<Map<String, String>> actual = new ArrayList<>();
    for (Keys.AtKey key : context.getAtClient(atSign).getAtKeys(regex, true).get()) {
      List<String> values = toDataTableRow(HEADINGS_INCLUDING_METADATA, key);
      Map<String, String> map = new HashMap<>();
      for (int i = 0; i < HEADINGS_INCLUDING_METADATA.size(); i++) {
        map.put(HEADINGS_INCLUDING_METADATA.get(i), values.get(i));
      }
      actual.add(map);
    }
    return actual;
  }

}
