import FCM.FcmServer;

import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.auth.FirebaseCredentials;
import com.google.firebase.database.*;
import com.google.gson.Gson;

import java.io.FileInputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

public class DatabaseAdmin {
    private static final Logger logger = Logger.getLogger(FcmServer.class.getName());

    public DatabaseAdmin() {
        FileInputStream serviceAccount = null;
        try {
            serviceAccount = new FileInputStream(VALUES.FILE_PATH_TO_DB_CREDENTIALS);
            // Initialize the app with a custom auth variable
            Map<String, Object> auth = new HashMap<String, Object>();
            auth.put("uid", VALUES.DB_ADMIN_AUTH_VALUE);
            FirebaseOptions options = new FirebaseOptions.Builder()
                    .setCredential(FirebaseCredentials.fromCertificate(serviceAccount))
                    .setDatabaseUrl(VALUES.FIREBASE_DB_URL)
                    .setDatabaseAuthVariableOverride(auth)
                    .build();

            FirebaseApp.initializeApp(options);
        } catch (IOException e) {e.printStackTrace();}

    }

    public void addNewTaskListener() {
        final FirebaseDatabase database = FirebaseDatabase.getInstance();
        DatabaseReference ref = database.getReference(VALUES.TASKS_PATH_DB);

        ref.addChildEventListener(new ChildEventListener() {
            @Override
            public void onChildAdded(DataSnapshot dataSnapshot, String prevChildKey) {
                //TODO task is converted to object, then json again
                Task newTask = dataSnapshot.getValue(Task.class);
                String taskId = dataSnapshot.getKey();
                String treatment = dataSnapshot.getRef().getParent().getKey();
                logger.log(Level.INFO, "New Task Added To DB, TASK ID::::" + taskId);
                if (newTask.getState().equalsIgnoreCase("new")) {
                    Main.sendTaskUseDatabase(newTask, taskId);
                }
                //allowing to repeated testing \/ \/ damn italics ruining my arrows c'mon
                //dataSnapshot.getRef().removeValue();
            }

            @Override
            public void onChildChanged(DataSnapshot dataSnapshot, String s) {

            }

            @Override
            public void onChildRemoved(DataSnapshot dataSnapshot) {

            }

            @Override
            public void onChildMoved(DataSnapshot dataSnapshot, String s) {

            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }

        });
    }

    public String addDummyTask() {
        DatabaseReference newTaskRef = FirebaseDatabase.getInstance().getReference(VALUES.TASKS_PATH_DB).push();
        Task dummyTask = new Task(50.1341, -1.4082, 50.0832, -1.4028, LocalDateTime.now().toString(), null,
                "Title Dummy", "Description of dummy task. It's tree fiddy on completion.", "NEW", null, 3.50);
        //String jsonTaskString = new Gson().toJson(dummyTask);
        newTaskRef.setValue(dummyTask);
        String taskId = newTaskRef.getKey();
        return taskId;
    }

    /*
     * One time database listener to listen for userTaskInfo messages from clients
     * Use taskId = "" to listen for all tasks
     */
    public void getMessagesForTask(String taskId) { //TODO STILL TO DO.. NOT COMPLETE..

        //Add listener for specific task
        logger.log(Level.INFO, "Adding listener to all tasks");
        final FirebaseDatabase database = FirebaseDatabase.getInstance();
        DatabaseReference messagesRef = database.getReference(VALUES.DB_MESSAGES_PATH).child(taskId);
        messagesRef.addListenerForSingleValueEvent(new ValueEventListener() {

            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {

                ArrayList<String> userIds = new ArrayList<>();
                //Iterate through all stored messages
                for (DataSnapshot userData : dataSnapshot.getChildren()) {
                    String userId = (String) userData.child("userId").getValue();
                    logger.log(Level.INFO, "taskId:userId - " + taskId + ":" + userId);
                    userIds.add(userId);
                }//todo no double loop?


                //TODO processing / selecting users
                //Can create UserTaskInfo Object using Gson and use these for processing

                //Can either send taskId and use can fetch data, or send task data to user
                final String[] jsonTaskData = new String[2]; //TODO WHAAT
                DatabaseReference ref = database.getReference(VALUES.TASKS_PATH_DB);
                ref.child(taskId).addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        logger.log(Level.INFO, "TaaaaASK EXISTS" + dataSnapshot.getRef() + dataSnapshot.getKey() + dataSnapshot.getValue());
                        jsonTaskData[0] = (String) dataSnapshot.getValue();
                        jsonTaskData[1] = (String) dataSnapshot.child("title").getValue();
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {
                        logger.log(Level.INFO, "task listener error: " + databaseError);
                    }
                });
                logger.log(Level.INFO, "Got task noti deets: " +
                        jsonTaskData[0] + "," + jsonTaskData[1] + " : " + userIds);
                //TODO SUPER MESSY!! refactor code
                //Main.sendTaskNotification(jsonTaskData[0], jsonTaskData[1], userIds);

                //removing message
                //dataSnapshot.getRef().removeValue();
                //Main.callDatabaseTest();
            }


            @Override
            public void onCancelled(DatabaseError databaseError) {
                logger.log(Level.INFO, "task listener2 error: " + databaseError);
            }
        });
    }

    /*
     * One time database listener to listen for userTaskInfo messages from clients
     * Use taskId = "" to listen for all tasks
     */
    public void getMessagesForAllTasks() {
        //Add listener for specific task
        logger.log(Level.INFO, "Adding listener to all tasks");
        final FirebaseDatabase database = FirebaseDatabase.getInstance();
        DatabaseReference messagesRef = database.getReference(VALUES.DB_MESSAGES_PATH);
        messagesRef.addListenerForSingleValueEvent(new ValueEventListener() {

            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {

                ArrayList<String> userIds = new ArrayList<>();
                //Iterate through all stored messages
                for (DataSnapshot taskData : dataSnapshot.getChildren()) {
                    //logger.log(Level.INFO, "DB tasksIds?: " + taskData.toString());
                    //String taskId = (String) data.child("taskId").getValue();
                    String taskId = taskData.getKey();
                    //String userId = (String) taskData.child("userId").getValue();
                    for (DataSnapshot userData : taskData.getChildren()) {
                        String userId = (String) userData.child("userId").getValue();
                        logger.log(Level.INFO, "taskId:userId - " + taskId + ":" + userId);
                        userIds.add(userId);
                    }//todo no double loop?


                    //TODO processing / selecting users
                    //Can create UserTaskInfo Object using Gson and use these for processing

                    //Can either send taskId and use can fetch data, or send task data to user

                    DatabaseReference ref = database.getReference(VALUES.TASKS_PATH_DB);
                    ref.child(taskId).addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(DataSnapshot dataSnapshot) {
                            logger.log(Level.INFO, "TASK EXISTS" + dataSnapshot.getRef() + dataSnapshot.getKey() + dataSnapshot.getValue());
                            Object value = dataSnapshot.getValue();
                            String jsonTaskString = new Gson().toJson(value);
                            String title = (String) dataSnapshot.child("title").getValue();
                            logger.log(Level.INFO, "Got task noti deets: " +
                                    value + "," + title + " : " + userIds);
                            //TODO SUPER MESSY!! refactor code
                            Main.sendTaskNotification(taskId, jsonTaskString, title, userIds);
                        }

                        @Override
                        public void onCancelled(DatabaseError databaseError) {
                            logger.log(Level.INFO, "DB Error: " + databaseError);
                        }
                    });



                }
            }


            @Override
            public void onCancelled(DatabaseError databaseError) {
            }
        });

        //removing messages for all tasks
        //messagesRef.removeValue();
        //Main.callDatabaseTest();
    }
}
