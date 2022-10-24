package org.atsign.client.api.notification;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import org.atsign.client.api.AtClient;
import org.atsign.client.api.AtEvents;
import org.atsign.client.api.impl.clients.NotificationServiceImpl;

public interface NotificationService {
    
    public static NotificationService create(AtClient atClient) {
        return new NotificationServiceImpl(atClient);
    }

    CompletableFuture<Object> subscribe(String regex, boolean shouldDecrypt); // TODO Replace "Object" with Stream<AtNotification> or similar. See: https://github.com/atsign-foundation/at_client_sdk/blob/99707459494594fbdc3f0873769eeca29c6a3124/packages/at_client/lib/src/service/notification_service.dart#L14   
    CompletableFuture<NotificationResult> notify(NotificationParams params);
    CompletableFuture<NotificationResult> getStatus(String notificationId);
    CompletableFuture<List<AtNotification>> notifyList();
    CompletableFuture<NotificationResult> notifyDelete(String notificationId);
    CompletableFuture<NotificationResult> notifyDeleteAll();

}
