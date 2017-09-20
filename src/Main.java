import FCM.FcmServer;
import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.XMPPException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.concurrent.CountDownLatch;

/**
 * This class acts as the controller class between the messaging and database.
 */
public class Main {
    private static FcmServer fcmServer;
    private static DatabaseAdmin databaseAdmin;

    public static void main(String[] args) {

        fcmServer = FcmServer.prepareClient(VALUES.FCM_SENDER_ID, VALUES.FCM_SERVER_KEY, true);

        try {
            fcmServer.connect();
        } catch (XMPPException | InterruptedException | IOException | SmackException e) {
            e.printStackTrace();
        }

        databaseSetup();

        try {
            CountDownLatch latch = new CountDownLatch(1);
            latch.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public static void databaseSetup() {
        databaseAdmin = new DatabaseAdmin();
        databaseAdmin.addNewTaskListener();
    }

    public static void sendTaskData(String jsonStringTask, String taskId, String toId) {
        fcmServer.sendTaskData(jsonStringTask, taskId, toId);
    }

    public static void sendTaskNotification(String jsonTaskString, String title, ArrayList<String> userIds){
        fcmServer.sendTaskNotification(userIds, jsonTaskString, title);
    }

    public static void getMessagesForTask(String taskId) {
        try {Thread.sleep(10000);} catch (InterruptedException e) {e.printStackTrace();}
        databaseAdmin.getMessagesForTask(taskId);
    }
}