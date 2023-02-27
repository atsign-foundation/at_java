
import java.util.List;
import java.util.concurrent.ExecutionException;

import org.atsign.client.api.AtClient;
import org.atsign.client.api.notification.AtNotification;
import org.atsign.client.api.notification.NotificationService;
import org.atsign.common.AtException;
import org.atsign.common.AtSign;

public class NotifyListExample {
    public static void main(String[] args) throws AtException, InterruptedException, ExecutionException {
        final String ROOT_URL = "root.atsign.org:64";
        final String ATSIGN_STR = "@smoothalligator";
        final AtSign atSign = new AtSign(ATSIGN_STR);
        final AtClient atClient = AtClient.withRemoteSecondary(ROOT_URL, atSign);

        final NotificationService ns = atClient.getNotificationService();
        final List<AtNotification> notifications = ns.notifyList().get();

        final int numNotifications = notifications.size();
        System.out.println("Notifications Listed: (" + numNotifications + ")");
        for(int i = 0; i < numNotifications; i++) {
            AtNotification notification = notifications.get(i);
            String id = notification.id;
            String from = notification.from;
            String key = notification.key;
            System.out.println("[" + i + "]: " + id + " | from: " + from + " | key: " + key);
        }
    }
}
