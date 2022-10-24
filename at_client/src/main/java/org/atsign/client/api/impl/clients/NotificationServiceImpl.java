package org.atsign.client.api.impl.clients;

import java.util.List;
import java.util.concurrent.CompletableFuture;


import org.atsign.client.api.AtClient;
import org.atsign.client.api.notification.AtNotification;
import org.atsign.client.api.notification.NotificationParams;
import org.atsign.client.api.notification.NotificationResult;
import org.atsign.client.api.notification.NotificationService;
import org.atsign.client.util.AtClientValidation;
import org.atsign.common.AtSign;
import org.atsign.common.NotificationStatus;

public class NotificationServiceImpl implements NotificationService {
    
    private final AtClient atClient;
    
    public NotificationServiceImpl(AtClient atClient) {
        this.atClient = atClient;
    }

    @Override
    public CompletableFuture<Object> subscribe(String regex, boolean shouldDecrypt) {
        throw new IllegalCallerException("Not implemented yet");
    }

    @Override

    public CompletableFuture<NotificationResult> notify(NotificationParams params) {
        
        return CompletableFuture.supplyAsync(() -> {
            NotificationResult result = new NotificationResult(params.getNotificationId(), params.getAtKey(), NotificationStatus.undelivered);
            if(params.getAtKey().sharedBy == null) {
                params.getAtKey().sharedBy = atClient.getAtSign();
            }
            
            return result;
        });
        
        
    }

    @Override
    public CompletableFuture<NotificationResult> getStatus(String notificationId) {
        throw new IllegalCallerException("Not implemented yet");
    }

    @Override
    public CompletableFuture<List<AtNotification>> notifyList() {
        throw new IllegalCallerException("Not implemented yet");
    }

    @Override
    public CompletableFuture<NotificationResult> notifyDelete(String notificationId) {
        throw new IllegalCallerException("Not implemented yet");
    }

    @Override
    public CompletableFuture<NotificationResult> notifyDeleteAll() {
        throw new IllegalCallerException("Not implemented yet");
    }

}
