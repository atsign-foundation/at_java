package org.atsign.client.impl.commands;

import static org.atsign.client.api.AtSign.createAtSign;
import static org.atsign.client.impl.util.EncryptionUtils.generateRSAKeyPair;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;

import org.atsign.client.api.AtCommandExecutor;
import org.atsign.client.api.AtEvents;
import org.atsign.client.api.AtKeys;
import org.atsign.client.api.AtSign;
import org.atsign.client.impl.exceptions.AtOnReadyException;
import org.atsign.client.impl.exceptions.AtTimeoutException;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.mockito.stubbing.Answer;

class NotificationsTest {

  @Test
  void testMonitorSendExpectedCommands() throws Exception {
    AtSign atSign = createAtSign("colin");
    AtKeys keys = AtKeys.builder().apkamKeyPair(generateRSAKeyPair()).build();
    AtCommandExecutor executor = mock(AtCommandExecutor.class);
    stubAuthentication(executor, atSign);

    Consumer<String> consumer = mock(Consumer.class);
    Notifications.monitor(executor, atSign, keys, consumer);

    verify(executor).sendSync(eq("monitor"), eq(consumer));
  }

  @Test
  void testMonitorWrapsConsumer() throws Exception {
    AtSign atSign = createAtSign("colin");
    AtKeys keys = AtKeys.builder().apkamKeyPair(generateRSAKeyPair()).build();
    AtCommandExecutor executor = mock(AtCommandExecutor.class);
    stubAuthentication(executor, atSign);
    Consumer<String> consumer = mock(Consumer.class);
    doAnswer((Answer<Void>) invocation -> {
      throw new AtTimeoutException("deliberate");
    }).when(executor).sendSync(eq("monitor"), Mockito.any(Consumer.class));

    Exception ex =
        assertThrows(Exception.class, () -> Notifications.monitor(atSign, keys, null, consumer).accept(executor));
    assertThat(ex, instanceOf(AtOnReadyException.class));
  }

  @Test
  void testMatchNotification() {
    Map<String, Object> eventData = Notifications.matchNotification("notification:{\"id\":\"-1\",\"from\":\"@gary\"}");
    assertThat(eventData.get("id"), equalTo("-1"));
    assertThat(eventData.get("from"), equalTo("@gary"));
  }

  @Test
  void testMatchNotificationThrowsException() {
    Exception ex = assertThrows(Exception.class, () -> Notifications.matchNotification("data:ok"));
    assertThat(ex.getMessage(), containsString("expected [notification:\\s*(\\{.+})] but input was : data:ok"));
  }

  @Test
  void testEventBusBridgePublishesExpectedEventForStatsNotification() {
    AtEvents.AtEventBus eventBus = mock(AtEvents.AtEventBus.class);
    Notifications.EventBusBridge consumer = new Notifications.EventBusBridge(eventBus, createAtSign("colin"));

    consumer.accept("notification: {\"id\":\"-1\",\"from\":\"@gary\",\"to\":\"@gary\"" +
        ",\"key\":\"statsNotification.@gary\",\"value\":\"229\"" +
        ",\"operation\":\"update\",\"epochMillis\":100000,\"messageType\":\"MessageType.key\"" +
        ",\"isEncrypted\":false,\"metadata\":null}");

    ArgumentCaptor<Map<String, Object>> captor = ArgumentCaptor.forClass(Map.class);
    verify(eventBus).publishEvent(eq(AtEvents.AtEventType.statsNotification), captor.capture());
    assertThat(captor.getValue().get("id"), equalTo("-1"));
    assertThat(captor.getValue().get("operation"), equalTo("update"));
    assertThat(captor.getValue().get("epochMillis"), equalTo(100000));
  }

  @Test
  void testEventBusBridgePublishesExpectedEventForSharedKeyNotification() {
    AtEvents.AtEventBus eventBus = mock(AtEvents.AtEventBus.class);
    Notifications.EventBusBridge consumer = new Notifications.EventBusBridge(eventBus, createAtSign("gary"));

    consumer.accept("notification: {\"id\":\"0480060d\",\"from\":\"@colin\"" +
        ",\"to\":\"@gary\",\"key\":\"@gary:shared_key@colin\"" +
        ",\"value\":\"xxx==\",\"operation\":\"update\",\"epochMillis\":1773077019848" +
        ",\"messageType\":\"MessageType.key\",\"isEncrypted\":false,\"metadata\":{\"encKeyName\":null" +
        ",\"encAlgo\":null,\"ivNonce\":null,\"skeEncKeyName\":null,\"skeEncAlgo\":null,\"sharedKeyEnc\":null" +
        ",\"pubKeyCS\":null,\"dataSignature\":null,\"pubKeyHash\":null}}");

    ArgumentCaptor<Map<String, Object>> captor = ArgumentCaptor.forClass(Map.class);
    verify(eventBus).publishEvent(eq(AtEvents.AtEventType.sharedKeyNotification), captor.capture());
    assertThat(captor.getValue().get("id"), equalTo("0480060d"));
    assertThat(captor.getValue().get("operation"), equalTo("update"));
    assertThat(captor.getValue().get("epochMillis"), equalTo(1773077019848L));
  }

  @Test
  void testEventBusBridgePublishesExpectedEventForUpdateNotification() {
    AtEvents.AtEventBus eventBus = mock(AtEvents.AtEventBus.class);
    Notifications.EventBusBridge consumer = new Notifications.EventBusBridge(eventBus, createAtSign("gary"));

    consumer.accept("notification: {\"id\":\"cc72371c\",\"from\":\"@colin\",\"to\":\"@gary\"" +
        ",\"key\":\"@gary:test@colin\",\"value\":\"C8gg7hDuJ4BVk6hrgu2GCQ==\",\"operation\":\"update\"" +
        ",\"epochMillis\":1773077220149,\"messageType\":\"MessageType.key\",\"isEncrypted\":true" +
        ",\"metadata\":{\"encKeyName\":null,\"encAlgo\":null,\"ivNonce\":\"yeZOQZleN6sXeVUuajn12w==\"" +
        ",\"skeEncKeyName\":null,\"skeEncAlgo\":null,\"sharedKeyEnc\":null,\"pubKeyCS\":null" +
        ",\"dataSignature\":null,\"pubKeyHash\":null}}");

    ArgumentCaptor<Map<String, Object>> captor = ArgumentCaptor.forClass(Map.class);
    verify(eventBus).publishEvent(eq(AtEvents.AtEventType.updateNotification), captor.capture());
    assertThat(captor.getValue().get("id"), equalTo("cc72371c"));
    assertThat(captor.getValue().get("operation"), equalTo("update"));
    assertThat(captor.getValue().get("epochMillis"), equalTo(1773077220149L));
  }

  @Test
  void testEventBusBridgeCatchesUnexpectedExceptions() {
    AtEvents.AtEventBus eventBus = mock(AtEvents.AtEventBus.class);
    Notifications.EventBusBridge consumer = new Notifications.EventBusBridge(eventBus, createAtSign("gary"));

    consumer.accept("xyz");

    verifyNoInteractions(eventBus);
  }

  @Test
  void testEventBusBridgePublishesExpectedEventForDeleteNotification() {
    AtEvents.AtEventBus eventBus = mock(AtEvents.AtEventBus.class);
    Notifications.EventBusBridge consumer = new Notifications.EventBusBridge(eventBus, createAtSign("gary"));

    consumer.accept("notification: {\"id\":\"41b265d8\",\"from\":\"@colin\",\"to\":\"@gary\"" +
        ",\"key\":\"@gary:test@colin\",\"value\":null,\"operation\":\"delete\",\"epochMillis\":1773077405537" +
        ",\"messageType\":\"MessageType.key\",\"isEncrypted\":true,\"metadata\":{\"encKeyName\":null" +
        ",\"encAlgo\":null,\"ivNonce\":null,\"skeEncKeyName\":null,\"skeEncAlgo\":null,\"sharedKeyEnc\":null" +
        ",\"pubKeyCS\":null,\"dataSignature\":null,\"pubKeyHash\":null}}");

    ArgumentCaptor<Map<String, Object>> captor = ArgumentCaptor.forClass(Map.class);
    verify(eventBus).publishEvent(eq(AtEvents.AtEventType.deleteNotification), captor.capture());
    assertThat(captor.getValue().get("id"), equalTo("41b265d8"));
    assertThat(captor.getValue().get("operation"), equalTo("delete"));
    assertThat(captor.getValue().get("epochMillis"), equalTo(1773077405537L));
  }

  @Test
  void testEventBusBridgePublishesExpectedEventForUnrecognizedNotification() {
    AtEvents.AtEventBus eventBus = mock(AtEvents.AtEventBus.class);
    Notifications.EventBusBridge consumer = new Notifications.EventBusBridge(eventBus, createAtSign("gary"));

    consumer.accept("notification: {\"id\":\"41b265d8\",\"from\":\"@colin\",\"to\":\"@gary\"" +
        ",\"key\":\"@gary:test@colin\",\"value\":null,\"operation\":\"unrecognized\",\"epochMillis\":1773077405537" +
        ",\"messageType\":\"MessageType.key\",\"isEncrypted\":true,\"metadata\":{\"encKeyName\":null" +
        ",\"encAlgo\":null,\"ivNonce\":null,\"skeEncKeyName\":null,\"skeEncAlgo\":null,\"sharedKeyEnc\":null" +
        ",\"pubKeyCS\":null,\"dataSignature\":null,\"pubKeyHash\":null}}");

    ArgumentCaptor<Map<String, Object>> captor = ArgumentCaptor.forClass(Map.class);
    verify(eventBus).publishEvent(eq(AtEvents.AtEventType.monitorException), captor.capture());
    assertThat(captor.getValue().get("key"), equalTo("__monitorException__"));
    assertThat(captor.getValue().get("exception"), equalTo("unknown notification operation 'unrecognized'"));
  }

  private static void stubAuthentication(AtCommandExecutor executor, AtSign atSign)
      throws ExecutionException, InterruptedException {
    when(executor.sendSync(anyString())).thenAnswer((Answer<String>) invocation -> {
      String command = invocation.getArgument(0);
      if (command.matches("from:" + atSign)) {
        return "data:challenge";
      } else if (command.matches("pkam:[^{].+")) {
        return "data:success";
      } else {
        throw new IllegalArgumentException("unexpected command : " + command);
      }
    });
  }


}
