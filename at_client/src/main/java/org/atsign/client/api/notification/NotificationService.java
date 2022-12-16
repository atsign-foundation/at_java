package org.atsign.client.api.notification;

import java.util.Date;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import org.atsign.client.api.AtClient;

public interface NotificationService {
    
    public static NotificationService create(AtClient atClient) {
        return new NotificationServiceImpl(atClient);
    }

    /**
     * TODO Javadoc
     * @param regex
     * @param shouldDecrypt
     * @return
     */
    CompletableFuture<Object> subscribe(String regex, boolean shouldDecrypt); // TODO Replace "Object" with Stream<AtNotification> or similar. See: https://github.com/atsign-foundation/at_client_sdk/blob/99707459494594fbdc3f0873769eeca29c6a3124/packages/at_client/lib/src/service/notification_service.dart#L14   

    /**
     * Sends a notification (via the `notify:` verb)
     * Use NotificationParams static factory methods to create an instance. 
     * @param params parameters containing details about the Notification that will be sent.
     * @return NotificationResult object containing details about the notification that was sent.
     */
    CompletableFuture<NotificationResult> notify(NotificationParams params);

    /**
     * Gets the status notification (via the `notify:status:` verb)
     * @param notificationId id of the notificaiton (e.g. `4a8bbbe9-3c7c-4679-9721-dfd01b5adf3f`)
     * @return NotificationResult object containing details about the status of the notification.
     */
    CompletableFuture<NotificationResult> getStatus(String notificationId);

    /**
     * Returns a List<AtNotification> object containing all the notifications received by the currently onboarded atSign.
     * Each AtNotification object represents a notification in the notification store.
     * @return List<AtNotification> object, non-null, emptyable
     */
    CompletableFuture<List<AtNotification>> notifyList();

    /**
     * Returns a List<AtNotification> object containing all the notifications received by the currently onboarded atSign.
     * Each AtNotification object represents a notification in the notification store.
     * @param regex regex filter when searching through notifications. Filters what is inside "key":
     * @return List<AtNotification> object, non-null, emptyable
     */
    CompletableFuture<List<AtNotification>> notifyList(String regex);

    /**
     * Returns a List<AtNotification> object containing all the notifications received by the currently onboarded atSign.
     * Each AtNotification object represents a notification in the notification store.
     * Filter notifications by regex and between a date range.
     * fromDate must be before toDate.
     * @param regex regex filter when searching through notifications. Filters what is inside "key":
     * @param fromDate Date object, only year, month, and day are considered
     * @param toDate Date object, only year, month, and day are considered
     * @return List<AtNotification> object, non-null, emptyable
     */
    CompletableFuture<List<AtNotification>> notifyList(String regex, Date fromDate, Date toDate);

    /**
     * Removes a notification given a String notificationId
     * @param notificationId id representing a notification inside the atSign's atServer notification store
     * @return NotificationResult object detailing the result of running this method
     */
    CompletableFuture<NotificationResult> notifyRemove(String notificationId);

    /**
     * Removes all notifications in the atServer's notification store
     * @return NotificationResult object detailing the result of running this method
     */
    CompletableFuture<NotificationResult> notifyRemoveAll();

}
