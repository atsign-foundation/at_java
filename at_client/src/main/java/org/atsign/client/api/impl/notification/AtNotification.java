package org.atsign.client.api.impl.notification;

import java.util.ArrayList;
import java.util.List;

import org.atsign.client.api.Secondary;
import org.atsign.common.ResponseTransformers.NotifyListResponseTransformer;
import org.atsign.common.response_models.NotifyListResponse;

/**
 * Class represents a notification in the atProtocol.
 */
public class AtNotification {
    
    public String id; // id of the notification
    public String key; // notification data
    public String from; // the atSign the notification is sent by
    public String to; // the atSign the notification is being sent to
    public Long epochMillis; // when the notification was sent in epoch millis
    public String value; // notification value, [OPTIONAL]
    public String operation; // "update", "delete", [OPTIONAL]
    public String messageType; // MessageType.Text or MessageType.Key
    public Boolean isEncrypted; // notification 

    @Override
    public String toString() {
        return "{\"id\": \"" + id + "\", \"key\": \"" + key + "\", \"from\": \"" + from + "\", \"to\": \"" + to + "\", \"epochMillis\": " + epochMillis.toString() + ", \"value\": \"" + value + "\", \"operation\": \"" + operation + "\", \"messageType\": \"" + messageType + "\", \"isEncrypted\": " + isEncrypted.toString() + "}";
    }

    /**
     * Convert the Secondary.Response into a List<AtNotification> object
     * @param response The response data from `data:<response data>` after running
     * `notify:list`
     * @return a List<AtNotification> where each AtNotification 
     */
    public static List<AtNotification> fromResponse(Secondary.Response response) {
        NotifyListResponseTransformer transformer = new NotifyListResponseTransformer();
        NotifyListResponse model = transformer.transform(response);
        List<AtNotification> notifications = new ArrayList<AtNotification>();
        model.notifications.stream().forEach((n) -> {
            AtNotification notification = new AtNotification();
            notification.id = n.id;
            notification.key = n.key;
            notification.from = n.from;
            notification.to = n.to;
            notification.epochMillis = n.epochMillis;
            notification.value = n.value;
            notification.operation = n.operation;
            notification.messageType = n.messageType;
            notification.isEncrypted = n.isEncrypted;
            notifications.add(notification);
        });
        return notifications;
    }

}
