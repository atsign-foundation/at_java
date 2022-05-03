package org.atsign.client.api;

import java.util.Map;
import java.util.Set;

/**
 *
 */
public interface AtEvents {
    interface AtEventListener {
        void handleEvent(AtEventType eventType, Map<String, Object> eventData);
    }

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
    enum AtEventType {
        // events which originate from the client's "Monitor" connection, which asynchronously
        // receives notification events from its secondary server
        // Notification events most interesting to developers:
        sharedKeyNotification,
        updateNotification,
        deleteNotification,

        // Notification events usually only consumed within the AtClient library
        statsNotification,
        monitorHeartbeatAck,
        monitorException,

        // events which originate from elsewhere
        decryptedUpdateNotification,

        userDefined
    }
}
