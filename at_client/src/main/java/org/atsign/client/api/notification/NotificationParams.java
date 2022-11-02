package org.atsign.client.api.notification;

import java.util.UUID;

import org.atsign.common.AtSign;
import org.atsign.common.KeyBuilders;
import org.atsign.common.Keys;
import org.atsign.common.NotificationEnums;
import org.atsign.common.Keys.AtKey;
import org.atsign.common.Keys.SharedKey;
import org.atsign.common.NotificationEnums.Operation;


public class NotificationParams {
    
    private String id;
    private AtKey atKey;
    private String value; // nullable (optional)
    private NotificationEnums.Operation operation;
    private NotificationEnums.MessageType messageType;
    private NotificationEnums.Priority priority;
    private NotificationEnums.Strategy strategy;
    // private Integer latestN = 1;
    // private String notifier = SYSTEM;

    public String getNotificationId() {
        return id;
    }

    public AtKey getAtKey() {
        return atKey;
    }

    public String getValue() {
        return value;
    }

    public NotificationEnums.Operation getOperation() {
        return operation;
    }

    public NotificationEnums.MessageType getMessageType() {
        return messageType;
    }

    public NotificationEnums.Priority getPriority() {
        return priority;
    }

    public NotificationEnums.Strategy getStrategy() {
        return strategy;
    }

    /**
     * 
     * @param atKey AtKey object which contains the sharedBy and sharedWith atSign.
     * @param value nullable (optional)
     * @return NotificationParams object
     */
    public static NotificationParams forUpdate(AtKey atKey, String value) {
        NotificationParams params = new NotificationParams();
        params.id = UUID.randomUUID().toString();
        params.atKey = atKey;
        params.value = value;
        params.operation = Operation.UPDATE;
        params.messageType = NotificationEnums.MessageType.KEY;
        params.priority = NotificationEnums.Priority.LOW;
        params.strategy = NotificationEnums.Strategy.ALL;
        return params;
    }

    public static NotificationParams forDelete(AtKey atKey) {
        NotificationParams params = new NotificationParams();
        params.id = UUID.randomUUID().toString();
        params.atKey = atKey;
        params.operation = Operation.DELETE;
        params.messageType = NotificationEnums.MessageType.KEY;
        params.priority = NotificationEnums.Priority.LOW;
        params.strategy = NotificationEnums.Strategy.ALL;
        return params;
    }

    public static NotificationParams forText(String text, AtSign sharedBy, AtSign sharedWith, Boolean shouldEncrypt) {

        SharedKey sharedKey = new KeyBuilders.SharedKeyBuilder(sharedBy, sharedWith).key(text).build();
        sharedKey.metadata.isEncrypted = shouldEncrypt;
        

        NotificationParams params = new NotificationParams();
        params.id = UUID.randomUUID().toString();
        params.atKey = sharedKey;
        params.operation = Operation.UPDATE;
        params.messageType = NotificationEnums.MessageType.TEXT;
        params.priority = NotificationEnums.Priority.LOW;
        params.strategy = NotificationEnums.Strategy.ALL;
        return params;
    }

}