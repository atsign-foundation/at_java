package org.atsign.cucumber.steps;

import io.cucumber.datatable.DataTable;
import io.cucumber.datatable.DataTableFormatter;
import io.cucumber.java.en.Then;
import org.atsign.client.api.AtClient;
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
import static org.testcontainers.shaded.com.google.common.util.concurrent.Uninterruptibles.sleepUninterruptibly;
import static org.testcontainers.shaded.org.awaitility.Awaitility.await;

public class MonitorSteps {

    private static final Logger LOGGER = LoggerFactory.getLogger(MonitorSteps.class);

    private final AtClientContext context;

    private final Map<String, Long> keyStatsValues = new ConcurrentHashMap<>();

    public MonitorSteps(AtClientContext context) {
        this.context = context;
    }

    @Then("{ordinal} {atsign} AtClient monitor receives the following")
    public void assertNotifications(Integer ordinal, AtSign clientAtSign, DataTable expected) throws Exception {
        AtClient atClient = context.lookupAtClient(clientAtSign, ordinal);
        assertNotifications(atClient, expected, 5);
    }

    @Then("{atsign} AtClient monitor receives the following")
    public void assertNotifications(AtSign clientAtSign, DataTable expected) throws Exception {
        AtClient atClient = context.lookupOnlyAtClient(clientAtSign);
        assertNotifications(atClient, expected, 5);
    }

    @Then("AtClient monitor receives the following")
    public void assertNotifications(DataTable table) throws Exception {
        AtClient atClient = context.lookupOnlyAtClient(context.getCurrentQualifiedAtSign().getAtSign());
        assertNotifications(atClient, table, 5);
    }

    private void assertNotifications(AtClient atClient, DataTable table, long awaitSeconds) throws Exception {
        assertThat(atClient.isMonitorRunning(), is(true));
        List<Map<String, String>> expected = table.asMaps();
        try {
            await().atMost(awaitSeconds, TimeUnit.SECONDS)
                .until(() -> testContains(context.getEventData(atClient), expected, false));
        } catch (Exception e) {

        }
        try {
            assertContains(context.getEventData(atClient), expected, false);
        } catch (Exception e) {
            dumpEventsReceived(atClient);
            throw e;
        } finally {
            context.clearEvents(atClient);
        }
    }

    @Then("{atsign} AtClient monitor does not receive any notifications")
    public void assertNoNotifications(AtSign clientAtSign) throws Exception {
        AtClient atClient = context.lookupOnlyAtClient(clientAtSign);
        assertNoNotifications(atClient);
    }

    @Then("AtClient monitor does NOT receive any notifications")
    public void assertNoNotifications() throws Exception {
        AtClient atClient = context.lookupAtClient(context.getCurrentQualifiedAtSign());
        assertNoNotifications(atClient);
    }

    private void assertNoNotifications(AtClient atClient) throws Exception {
        assertThat(atClient.isMonitorRunning(), is(false));
        sleepUninterruptibly(2, TimeUnit.SECONDS);
        try {
            assertThat(context.getEventData(atClient), is(empty()));
        } catch (Exception e) {
            dumpEventsReceived(atClient);
            throw e;
        } finally {
            context.clearEvents(atClient);
        }
    }

    @Then("{atsign} AtClient monitor receives a new statsNotification")
    public void assertNewStatsNotification(AtSign clientAtSign) throws Exception {
        AtClient atClient = context.lookupOnlyAtClient(clientAtSign);
        String key = String.format("statsNotification.%s", clientAtSign.toString());
        assertNewStatsNotification(atClient, key, 5);
    }

    @Then("AtClient monitor receives a new statsNotification")
    public void assertNewStatsNotification() throws Exception {
        QualifiedAtSign currentQualifiedAtSign = context.getCurrentQualifiedAtSign();
        AtClient atClient = context.lookupAtClient(currentQualifiedAtSign);
        String key = String.format("statsNotification.%s", currentQualifiedAtSign.getAtSign().toString());
        assertNewStatsNotification(atClient, key, 5);
    }

    private void assertNewStatsNotification(AtClient atClient, String key, long awaitSeconds) throws Exception {
        long existingValue = keyStatsValues.getOrDefault(key, 0L);
        try {
            await().atMost(awaitSeconds, TimeUnit.SECONDS)
                .until(() -> getNewStatsNotificationValue(atClient, key, existingValue).isPresent());
            long newValue = getNewStatsNotificationValue(atClient, key, existingValue).get();
            assertThat(newValue, greaterThan(existingValue));
            keyStatsValues.put(key, newValue);
        } catch (Exception e) {
            dumpEventsReceived(atClient);
            throw e;
        } finally {
            context.clearEvents(atClient);
        }
    }

    private Optional<Long> getNewStatsNotificationValue(AtClient atClient, String key, long existingValue) throws Exception {
        return context.getEventData(atClient, AtEvents.AtEventType.statsNotification).stream()
            .filter(map -> map.containsValue(key))
            .map(map -> Long.valueOf(map.get("value")))
            .filter(value -> value > existingValue)
            .findAny();
    }

    private void dumpEventsReceived(AtClient atClient) throws Exception {
        List<Map<String, String>> eventData = context.getEventData(atClient);
        List<String> headings = new ArrayList<>();
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
