
import java.util.Scanner;
import java.util.concurrent.ExecutionException;


import org.atsign.client.api.AtClient;
import org.atsign.client.api.notification.NotificationResult;
import org.atsign.client.api.notification.NotificationService;
import org.atsign.common.AtException;
import org.atsign.common.AtSign;

public class NotifyRemoveExample {

    public static void main(String[] args) throws AtException, InterruptedException, ExecutionException {
        final String ROOT_URL = "root.atsign.org:64";
        final String ATSIGN_STR = "@smoothalligator";
        final AtSign atSign = new AtSign(ATSIGN_STR);
        final AtClient atClient = AtClient.withRemoteSecondary(ROOT_URL, atSign);

        final NotificationService ns = atClient.getNotificationService();

        Scanner scanner = new Scanner(System.in);
        System.out.println("Enter notification id to delete: ");
        String notificationId = scanner.nextLine();
        scanner.close();
        NotificationResult result = ns.notifyRemove(notificationId).get();
        System.out.println(result.toString());
    }
    
}
