package org.atsign.client.api.notification;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

import org.atsign.client.api.AtClient;
import org.atsign.client.api.Secondary;
import org.atsign.client.util.AtClientValidation;
import org.atsign.common.AtException;
import org.atsign.common.AtSign;
import org.atsign.common.NotificationEnums;
import org.atsign.common.NotificationStatus;
import org.atsign.common.ResponseTransformers.NotifyListResponseTransformer;
import org.atsign.common.VerbBuilders.NotifyRemoveVerbBuilder;
import org.atsign.common.VerbBuilders.NotifyKeyChangeBuilder;
import org.atsign.common.VerbBuilders.NotifyListVerbBuilder;
import org.atsign.common.VerbBuilders.NotifyTextVerbBuilder;
import org.atsign.common.VerbBuilders.VerbBuilder;
import org.atsign.common.response_models.NotifyListResponse;

public class NotificationServiceImpl implements NotificationService {

    private final AtClient atClient;

    public NotificationServiceImpl(AtClient atClient) {
        this.atClient = atClient;
    }

    @Override
    public CompletableFuture<Object> subscribe(String regex, boolean shouldDecrypt) {
        throw new RuntimeException("Not implemented yet");
    }

    @Override

    public CompletableFuture<NotificationResult> notify(NotificationParams params) {
        return CompletableFuture.supplyAsync(() -> {
            NotificationResult result = new NotificationResult(params.getNotificationId(), params.getAtKey(),
                    NotificationStatus.undelivered);

            if (params.getAtKey().sharedBy == null) {
                params.getAtKey().sharedBy = atClient.getAtSign();
            }

            // TODO: AtClientValidation notification

            VerbBuilder builder;
            switch (params.getMessageType()) {
                case TEXT:
                    NotifyTextVerbBuilder a = new NotifyTextVerbBuilder();
                    a.setRecipientAtSign(params.getAtKey().sharedWith.atSign);
                    a.setText(params.getAtKey().name);
                    builder = a;
                    break;
                case KEY:
                    NotifyKeyChangeBuilder b = new NotifyKeyChangeBuilder();
                    if (params.getOperation().toString().equalsIgnoreCase("update")
                            || params.getOperation().toString().equalsIgnoreCase("delete")) {
                        b.setOperation(params.getOperation().toString().toLowerCase());
                    }
                    b.setRecipientAtSign(params.getAtKey().sharedWith.atSign);
                    b.setKey(params.getAtKey().toString());
                    b.setSenderAtSign(params.getAtKey().sharedBy.atSign);
                    b.setValue(params.getValue());
                    b.setTtr(params.getAtKey().metadata.ttr);
                    builder = b;
                    break;
                default:
                    throw new IllegalArgumentException("Invalid message type");
            }

            String command = builder.build();
            try {
                Secondary.Response response = (atClient).executeCommand(command, false);
                // System.out.println("Executed command: " + command + " got: " + response.toString());
                if (response.data != "null") {
                    result.setNotificationStatus(NotificationStatus.delivered);
                }
            } catch (AtException e) {
                result.setNotificationStatus(NotificationStatus.errored);
            }
            return result;
        });
    }

    @Override
    public CompletableFuture<NotificationResult> getStatus(String notificationId) {
        throw new RuntimeException("Not implemented yet");
    }

    /**
     * Get the list of notifications (notify:list verb)
     * 
     * @return non-null List<AtNotification> object, emptyable
     */
    @Override
    public CompletableFuture<List<AtNotification>> notifyList() {
        return this.notifyList(null);
    }

    /**
     * Get the list of notifications (notify:list verb)
     * 
     * @param regex - regex to filter the notifications, nullable
     * @return non-null List<AtNotification> object, emptyable
     */
    public CompletableFuture<List<AtNotification>> notifyList(String regex) {
        return this.notifyList(regex, null, null);
    }

    /**
     * Get the list of notifications (notify:list verb)
     * 
     * @param regex    - regex to filter the notifications, nullable
     * @param fromDate - from date to filter the notifications yyyy-MM-dd (e.g.
     *                 "2021-01-01"), nullable
     * @param toDate   - to date to filter the notifications yyyy-MM-dd (e.g.
     *                 "2021-01-01"), nullable
     * @return non-null List<AtNotification> object, emptyable
     */
    public CompletableFuture<List<AtNotification>> notifyList(String regex, Date fromDate, Date toDate) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                List<AtNotification> atNotifications = getNotifications(regex, fromDate, toDate);
                return atNotifications;
            } catch (AtException e) {
                throw new CompletionException(e);
            }
        });
    }

    /**
     * Remove a notification from the notification store.
     * Each notification has a notificationId. Obtain these ids from notifyList(...)
     * @param notificationId - notificationId to remove from the notification store
     * @return NotificationResult object detailing the status of the notification. The status of the notification will be `delivered` as long as there was a response from the server. A `delivered` status does not mean the notification existed in the first place.
     */
    @Override
    public CompletableFuture<NotificationResult> notifyRemove(String notificationId) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return deleteNotification(notificationId);
            } catch (AtException e) {
                throw new CompletionException(e);
            }
        });
    }

    @Override
    public CompletableFuture<NotificationResult> notifyRemoveAll() {
        throw new RuntimeException("Not implemented yet");
    }

    /// ******************************
    /// Private Methods
    /// ******************************

    private NotificationResult deleteNotification(String notificationId) throws AtException {
        NotifyRemoveVerbBuilder b = new NotifyRemoveVerbBuilder();
        b.setNotificationId(notificationId);
        String command = b.build();


        NotificationResult result = new NotificationResult(notificationId, null,
                NotificationStatus.undelivered);

        Secondary.Response response = (atClient).executeCommand(command, false);
        if (response.data.equals("success")) {
            result.setNotificationStatus(NotificationStatus.delivered);
        }
        return result;
    }

    private List<AtNotification> getNotifications(String regex, Date fromDate, Date toDate) throws AtException {
        List<AtNotification> notifications = new ArrayList<AtNotification>();

        NotifyListVerbBuilder b = new NotifyListVerbBuilder();
        b.setRegex(regex);
        b.setFrom(fromDate);
        b.setTo(toDate);
        String command = b.build();

        Secondary.Response response = (atClient).executeCommand(command, false);

        notifications = AtNotification.fromResponse(response);

        return notifications;
    }

}
