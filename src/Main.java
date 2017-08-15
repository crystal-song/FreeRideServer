import FCM.DownstreamMessage;
import FCM.FcmServer;
import FCM.MessageHelper;
import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.XMPPException;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;

public class Main {
    private static FcmServer fcmServer;

    public static void main(String[] args) {

        fcmServer = FcmServer.prepareClient(VALUES.FCM_SENDER_ID, VALUES.FCM_SERVER_KEY, true);

        try {
            fcmServer.connect();
        } catch (XMPPException | InterruptedException | IOException | SmackException e) {
            e.printStackTrace();
        }


        String messageId = FcmServer.getUniqueMessageId();
        Map<String, String> dataPayload = new HashMap<String, String>();
        dataPayload.put("count", String.valueOf(1));
        dataPayload.put("messageType", "reply-test");
        DownstreamMessage message = new DownstreamMessage("/topics/client", messageId, dataPayload);
        message.setTimeToLive(10);
        message.setPriority("high");
        String jsonRequest = MessageHelper.createJsonDownstreamMessage(message);
		fcmServer.send(jsonRequest);

        DatabaseAdmin databaseAdmin = new DatabaseAdmin();
        //databaseAdmin.addNewTaskListener();
        //databaseAdmin.addDummyTask();




        try {
            CountDownLatch latch = new CountDownLatch(1);
            latch.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public static void replyTestMessage() {

    }

    public static void sendTaskToAll(Task task) {
        String messageId = FcmServer.getUniqueMessageId();
        Map<String, String> dataPayload = new HashMap<String, String>();
        dataPayload.put("messageType", "new-task");
        dataPayload.put("task", task.toString());
        DownstreamMessage message = new DownstreamMessage("/topics/client", messageId, dataPayload);
        message.setTimeToLive(10);
        message.setPriority("high");
        String jsonRequest = MessageHelper.createJsonDownstreamMessage(message);
        fcmServer.send(jsonRequest);
    }
}
