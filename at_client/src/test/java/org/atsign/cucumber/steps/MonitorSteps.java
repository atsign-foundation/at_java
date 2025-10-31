package org.atsign.cucumber.steps;

import io.cucumber.datatable.DataTable;
import io.cucumber.datatable.DataTableFormatter;
import io.cucumber.java.en.Then;
import org.atsign.client.api.AtEvents;
import org.atsign.common.AtSign;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.shaded.com.google.common.util.concurrent.Uninterruptibles;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import static org.atsign.cucumber.helpers.Helpers.assertContains;
import static org.atsign.cucumber.helpers.Helpers.testContains;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.testcontainers.shaded.org.awaitility.Awaitility.await;

public class MonitorSteps {

  private static final Logger LOGGER = LoggerFactory.getLogger(MonitorSteps.class);

  private final AtClientContext context;

  private final Map<String, Long> keyStatsValues = new ConcurrentHashMap<>();

  public MonitorSteps(AtClientContext context) {
    this.context = context;
  }

  @Then("{atsign} AtClient monitor receives the following")
  public void assertNotifications(AtSign atSign, DataTable table) throws Exception {
    assertThat(context.getAtClient(atSign).isMonitorRunning(), is(true));
    List<Map<String, String>> expected = table.asMaps();
    try {
      await().atMost(2, TimeUnit.SECONDS)
          .until(() -> testContains(context.getEventData(atSign), expected, false));
    } catch (Exception e) {

    }
    try {
      assertContains(context.getEventData(atSign), expected, false);
    } catch (Exception e) {
      dumpEventsReceived(atSign);
      throw e;
    } finally {
      context.clearEvents(atSign);
    }
  }

  @Then("AtClient monitor receives the following")
  public void assertNotifications(DataTable table) throws Exception {
    assertNotifications(context.getCurrentAtSign(), table);
  }

  @Then("{atsign} AtClient monitor does not receive any notifications")
  public void assertNoNotifications(AtSign atSign) throws Exception {
    assertThat(context.getAtClient(atSign).isMonitorRunning(), is(false));
    Uninterruptibles.sleepUninterruptibly(2, TimeUnit.SECONDS);
    try {
      List<Map<String, String>> eventData = context.getEventData(atSign);
      assertThat(eventData, is(empty()));
    } catch (Exception e) {
      dumpEventsReceived(atSign);
      throw e;
    } finally {
      context.clearEvents(atSign);
    }
  }

  @Then("AtClient monitor does NOT receive any notifications")
  public void assertNoNotifications() throws Exception {
    assertNoNotifications(context.getCurrentAtSign());
  }

  @Then("{atsign} AtClient monitor receives a new statsNotification")
  public void assertNewStatsNotification(AtSign atSign) throws Exception {
    String key = String.format("statsNotification.%s", atSign.toString());
    long existingValue = keyStatsValues.getOrDefault(key, 0L);
    try {
      await().atMost(3, TimeUnit.SECONDS)
          .until(() -> getNewStatsNotificationValue(atSign, key, existingValue).isPresent());
      long newValue = getNewStatsNotificationValue(atSign, key, existingValue).get();
      assertThat(newValue, greaterThan(existingValue));
      keyStatsValues.put(key, newValue);
    } catch (Exception e) {
      dumpEventsReceived(atSign);
      throw e;
    } finally {
      context.clearEvents(atSign);
    }
  }

  private Optional<Long> getNewStatsNotificationValue(AtSign atSign, String key, long existingValue) throws Exception {
    return context.getEventData(atSign, AtEvents.AtEventType.statsNotification).stream()
        .filter(map -> map.containsValue(key))
        .map(map -> Long.valueOf(map.get("value")))
        .filter(value -> value > existingValue)
        .findAny();
  }

  @Then("AtClient monitor receives a new statsNotification")
  public void assertNewStatsNotification() throws Exception {
    assertNewStatsNotification(context.getCurrentAtSign());
  }

  private void dumpEventsReceived(AtSign atSign) throws Exception {
    List<Map<String, String>> eventData = context.getEventData(atSign);
    List<String> headings = new ArrayList<>();
//    headings.add("Event Type");
    Set<String> eventDataKeys = new LinkedHashSet<>();
    eventData.forEach(event -> eventDataKeys.addAll(event.keySet()));
    headings.addAll(eventDataKeys);

    List<List<String>> raw = new ArrayList<>();
    raw.add(headings);
    for (Map<String, String> map : eventData) {
      List<String> row = new ArrayList<>();
      for (String key : eventDataKeys) {
        Object value = map.get(key);
        row.add(value != null ? value.toString() : "");
      }
      raw.add(row);
    }
    DataTableFormatter.builder()
        .prefixRow("  ")
        .escapeDelimiters(true)
        .build()
        .formatTo(DataTable.create(raw), System.out);
  }
}
