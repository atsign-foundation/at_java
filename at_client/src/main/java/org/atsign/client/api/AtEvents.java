package org.atsign.client.api;

import java.util.Map;
import java.util.Set;

/**
 * Parent interface for related interfaces
 */
public interface AtEvents {

  /**
   * Represents something that will receive events
   */
  interface AtEventListener {
    void handleEvent(AtEventType eventType, Map<String, Object> eventData);
  }

  /**
   * Represents something that can dispatch events to registered {@link AtEventListener}
   */
  interface AtEventBus {
    /**
     * @param listener to handle various events which originate from Secondaries
     * @param eventTypes the set of EventTypes that the listener is interested in
     */
    void addEventListener(AtEventListener listener, Set<AtEventType> eventTypes);

    /**
     * @param listener the listener to remove
     */
    void removeEventListener(AtEventListener listener);

    int publishEvent(AtEventType eventType, Map<String, Object> eventData);

  }

  /**
   * Different event types
   */
  enum AtEventType {
    // events which originate from the client's "Monitor" connection, which asynchronously
    // receives notification events from its secondary server
    // Monitor-originating events most interesting to developers:
    sharedKeyNotification, // a shared key has been updated by some other atSign
    updateNotification, // a value has been updated for some key
    deleteNotification, // some key has been deleted
    updateNotificationText, // an in-the-clear notification, no encryption
    // Monitor-originating events usually only consumed within the AtClient library
    statsNotification, //
    monitorHeartbeatAck, //
    monitorException, //

    // Event which originates from AtClient - AtClient listens for updateNotification events,
    // does decryption on the fly, then publishes a decryptedUpdateNotification event
    decryptedUpdateNotification,

    userDefined
  }
}
