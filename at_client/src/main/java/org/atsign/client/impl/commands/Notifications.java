package org.atsign.client.impl.commands;

import static org.atsign.client.api.AtEvents.AtEventType.*;
import static org.atsign.client.impl.commands.AtExceptions.throwOnReadyException;
import static org.atsign.client.impl.commands.AuthenticationCommands.authenticateWithPkam;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;
import java.util.regex.Pattern;

import lombok.extern.slf4j.Slf4j;
import org.atsign.client.api.AtEvents;
import org.atsign.client.api.AtKeys;
import org.atsign.client.api.AtCommandExecutor;
import org.atsign.client.impl.exceptions.AtException;
import org.atsign.client.api.AtSign;

/**
 * Utility methods for managing notifications within the AtSign protocol
 */
public class Notifications {

  /**
   * models server response string which is non-empty JSON map
   */
  protected static final Pattern NOTIFICATION_JSON_NON_EMPTY_MAP = Pattern.compile("notification:\\s*(\\{.+})");

  public static Consumer<AtCommandExecutor> monitor(AtSign atSign, AtKeys keys, Consumer<String> consumer) {
    return throwOnReadyException(executor -> monitor(executor, atSign, keys, consumer));
  }

  public static void monitor(AtCommandExecutor executor, AtSign atSign, AtKeys keys, Consumer<String> consumer)
      throws AtException {
    try {

      // authenticate
      authenticateWithPkam(executor, atSign, keys);

      // send monitor command
      executor.sendSync("monitor", consumer);

    } catch (ExecutionException | InterruptedException e) {
      throw new RuntimeException(e);
    }
  }

  public static Map<String, Object> matchNotification(String s) {
    return Responses.match(s, NOTIFICATION_JSON_NON_EMPTY_MAP, Responses::decodeJsonMapOfObjects);
  }

  /**
   * A {@link Consumer} that "bridges" to the {@link AtEvents.AtEventBus} api. This is intended
   * to be used for invoking {@link AtCommandExecutor#send(String, Consumer, CompletableFuture)}
   *
   */
  @Slf4j
  public static class EventBusBridge implements Consumer<String> {

    private final AtEvents.AtEventBus eventBus;

    private final AtSign atSign;

    public EventBusBridge(AtEvents.AtEventBus eventBus, AtSign atSign) {
      this.eventBus = eventBus;
      this.atSign = atSign;
    }

    @Override
    public void accept(String s) {
      try {
        Map<String, Object> eventData = matchNotification(s);
        AtEvents.AtEventType eventType = toEventType(eventData);
        if (eventType == monitorException) {
          eventData.put("key", "__monitorException__");
          eventData.put("value", s);
          eventData.put("exception", "unknown notification operation '" + eventData.get("operation") + "'");
        }
        eventBus.publishEvent(eventType, eventData);
      } catch (Exception e) {
        log.error("unexpected exception processing : {}", s, e);
      }
    }

    private AtEvents.AtEventType toEventType(Map<String, Object> eventData) {
      String id = (String) eventData.get("id");
      String operation = (String) eventData.get("operation");
      if ("-1".equals(id)) {
        return statsNotification;
      } else if ("update".equals(operation)) {
        String key = (String) eventData.get("key");
        if (key.startsWith(atSign + ":shared_key@")) {
          return sharedKeyNotification;
        } else {
          return updateNotification;
        }
      } else if ("delete".equals(operation)) {
        return deleteNotification;
      }
      return monitorException;
    }
  }
}
