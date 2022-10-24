package org.atsign.client.api.notification;

import org.atsign.common.NotificationStatus;
import org.atsign.common.Keys.AtKey;

public class NotificationResult {
    private String notificationId;
    private AtKey atKey;
    private NotificationStatus notificationStatus;

    public NotificationResult(String notificationId, AtKey atKey, NotificationStatus notificationStatus) {
        this.notificationId = notificationId;
        this.atKey = atKey;
        this.notificationStatus = notificationStatus;
    }

    public void setAtKey(AtKey atKey) {
        this.atKey = atKey;
    }

    public void setNotificationId(String notificationId) {
        this.notificationId = notificationId;
    }

    public void setNotificationStatus(NotificationStatus notificationStatus) {
        this.notificationStatus = notificationStatus;
    }

    public AtKey getAtKey() {
        return atKey;
    }

    public String getNotificationId() {
        return notificationId;
    }

    public NotificationStatus getNotificationStatus() {
        return notificationStatus;
    }

    @Override
    public String toString() {
        return "id: " + notificationId  + " | status: " + notificationStatus.name();
    }
    
}
