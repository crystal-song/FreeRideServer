import FCM.DownstreamMessage;
import FCM.FcmServer;
import FCM.MessageHelper;

import com.google.gson.Gson;

import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.XMPPException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;

public class Main {
    private static FcmServer fcmServer;
    private static DatabaseAdmin databaseAdmin;
    private static int count;

    public static void main(String[] args) {

        fcmServer = FcmServer.prepareClient(VALUES.FCM_SENDER_ID, VALUES.FCM_SERVER_KEY, true);

        try {
            fcmServer.connect();
        } catch (XMPPException | InterruptedException | IOException | SmackException e) {
            e.printStackTrace();
        }


        count = 0;


        databaseOperations();





        try {
            CountDownLatch latch = new CountDownLatch(1);
            latch.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public static void sendTaskUseDatabase(Task task, String taskId) {
        String jsonStringTask = new Gson().toJson(task);
        fcmServer.sendTaskData(jsonStringTask, taskId, VALUES.TOPICS_TEST);
    }

    public static void sendTaskNotification(String taskId, String jsonTaskString, String title, ArrayList<String> userIds){
        for (String userId : userIds) {
            fcmServer.sendTaskNotification(taskId ,userId, jsonTaskString, title);
        }
    }

    public static void databaseOperations() {
        databaseAdmin = new DatabaseAdmin();
        databaseAdmin.addNewTaskListener();
        String taskId = databaseAdmin.addDummyTask();
        System.out.println("Task Id: " + taskId);
        try {Thread.sleep(10000);} catch (InterruptedException e) {e.printStackTrace();}

        databaseAdmin.getMessagesForAllTasks();

    }

    public static void callDatabaseTest() {
        if (count < 10) {
            databaseAdmin.addDummyTask();
            //try {Thread.sleep(1000);} catch (InterruptedException e) {e.printStackTrace();}
            databaseAdmin.getMessagesForAllTasks();
            count++;
        }

    }


    public static void replyTestMessage() {
        String messageId = FcmServer.getUniqueMessageId();
        Map<String, String> dataPayload = new HashMap<String, String>();
        dataPayload.put("count", String.valueOf(1));
        dataPayload.put("messageType", "reply-test");
        DownstreamMessage message = new DownstreamMessage(VALUES.TOPICS_TEST, messageId, dataPayload);
        message.setTimeToLive(10);
        message.setPriority("high");
        String jsonRequest = MessageHelper.createJsonDownstreamMessage(message);
        fcmServer.send(jsonRequest);
    }

    public static void sendTaskToAll(Task task, String taskId) {

        String jsonStringTask = new Gson().toJson(task);


        /*
        Problem with upstream messaging: taking way too long.
        Haven't been able to find a solution yet, might find one later.
        Going to send task notification to all users instead.


        fcmServer.sendTaskData(jsonStringTask, taskId, VALUES.TOPICS_TEST);


        //wait for all replies to come in and be handled
        try {Thread.sleep(1000);} catch (InterruptedException e) { e.printStackTrace(); }


        //Opens connection to clients to receive their outgoing messages (get replies)
        fcmServer.sendPingHack(VALUES.TOPICS_TEST);


        //wait for all replies to come in and be handled
        try {Thread.sleep(1000);} catch (InterruptedException e) { e.printStackTrace(); }

        fcmServer.sendTaskNotification(jsonStringTask, task.getTitle());
        */


        //fcmServer.sendTaskNotification(VALUES.TOPICS_TEST, jsonStringTask, task.getTitle());


        //try {Thread.sleep(10000);} catch (InterruptedException e) { e.printStackTrace(); }

        //TODO if task not accepted by anyone, send to more users / move in DB
    }

    public static String getRandomTaskId() {
        return "taskId_" + UUID.randomUUID().toString();
    }
}
