
import java.util.concurrent.ExecutionException;

import org.atsign.client.api.AtClient;
import org.atsign.client.api.notification.NotificationParams;
import org.atsign.client.api.notification.NotificationResult;
import org.atsign.client.api.notification.NotificationService;
import org.atsign.client.api.notification.NotificationServiceImpl;
import org.atsign.common.AtException;
import org.atsign.common.AtSign;

public class NotifySendExample {

    public static void main(String[] args) throws AtException, InterruptedException, ExecutionException {
        
        // initialize AtClient instance
        final String ROOT_URL = "root.atsign.org:64";
        final String ATSIGN_STR = "@soccer0";
        final AtSign atSign = new AtSign(ATSIGN_STR);
        final AtClient atClient = AtClient.withRemoteSecondary(ROOT_URL, atSign);

        // get notification service 
        final NotificationService ns = (NotificationServiceImpl) atClient.getNotificationService();

        final String RECIPIENT_ATSIGN_STR = "@smoothalligator";
        final AtSign recipient = new AtSign(RECIPIENT_ATSIGN_STR);
        final NotificationResult result = ns.notify(NotificationParams.forText("12345", atSign, recipient, false)).get();

        System.out.println(result.toString());



    }
    
}
