package org.atsign.client.api.impl.clients;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

import org.atsign.client.api.AtClient;
import org.atsign.client.api.Secondary;
import org.atsign.client.api.notification.AtNotification;
import org.atsign.client.api.notification.NotificationParams;
import org.atsign.client.api.notification.NotificationResult;
import org.atsign.client.api.notification.NotificationService;
import org.atsign.client.util.AtClientValidation;
import org.atsign.common.AtException;
import org.atsign.common.AtSign;
import org.atsign.common.NotificationEnums;
import org.atsign.common.NotificationStatus;
import org.atsign.common.ResponseTransformers.NotifyListResponseTransformer;
import org.atsign.common.VerbBuilders.NotifyDeleteVerbBuilder;
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

    @Override
    public CompletableFuture<List<AtNotification>> notifyList() {
        return this.notifyList(null);
    }

    public CompletableFuture<List<AtNotification>> notifyList(String regex) {
        return this.notifyList(null, null);
    }

    public CompletableFuture<List<AtNotification>> notifyList(String regex, String fromDate) {
        return this.notifyList(regex, fromDate, null);
    }

    /**
     * Get the list of notifications (notify:list verb)
     * 
     * @param regex    - regex to filter the notifications, nullable
     * @param fromDate - from date to filter the notifications yyyy-MM-dd (e.g.
     *                 "2021-01-01"), nullable
     * @param toDate   - to date to filter the notifications yyyy-MM-dd (e.g.
     *                 "2021-01-01"), nullable
     * @return non-null List<AtNotification> object
     */
    public CompletableFuture<List<AtNotification>> notifyList(String regex, String fromDate, String toDate) {
        return CompletableFuture.supplyAsync(() -> {
            try {

                List<AtNotification> notifications = new ArrayList<AtNotification>();

                NotifyListVerbBuilder b = new NotifyListVerbBuilder();
                b.setRegex(regex);
                b.setFrom(fromDate);
                b.setTo(toDate);
                String command = b.build();

                Secondary.Response response = (atClient).executeCommand(command, false);

                notifications = AtNotification.fromResponse(response);

                return notifications;
            } catch (AtException e) {
                throw new CompletionException(e);
            }

        });
    }

    @Override
    public CompletableFuture<NotificationResult> notifyDelete(String notificationId) {

        return CompletableFuture.supplyAsync(() -> {
            try {
                NotifyDeleteVerbBuilder b = new NotifyDeleteVerbBuilder();
                b.setNotificationId(notificationId);
                String command = b.build();
                NotificationResult result = new NotificationResult(notificationId, null,
                        NotificationStatus.undelivered);

                Secondary.Response response = (atClient).executeCommand(command, false);
                if (response.data != "null") {
                    result.setNotificationStatus(NotificationStatus.delivered);
                }
                return result;
            } catch (AtException e) {
                throw new CompletionException(e);
            }
        });
    }

    @Override
    public CompletableFuture<NotificationResult> notifyDeleteAll() {
        throw new RuntimeException("Not implemented yet");
    }

}
