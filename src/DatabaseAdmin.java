import FCM.FcmServer;

import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.auth.FirebaseCredentials;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.io.FileInputStream;
import java.io.IOException;
import java.time.LocalDateTime;
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
                Task newTask = dataSnapshot.getValue(Task.class);
                String treatment = dataSnapshot.getRef().getParent().getKey();
                logger.log(Level.INFO, "New Task Listener: " + newTask.toString());
                if (newTask.getState().equalsIgnoreCase("new")) {
                    Main.sendTaskToAll(newTask);
                }
                //allowing to repeated testing \/ \/ damn italics ruining my arrows c'mon
                dataSnapshot.getRef().removeValue();
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

    public void addDummyTask() {
        DatabaseReference newTaskRef = FirebaseDatabase.getInstance().getReference(VALUES.TASKS_PATH_DB).push();
        Task dummyTask = new Task(50.1341, -1.4082, 50.0832, -1.4028, LocalDateTime.now().toString(), null,
                "Title Dummy", "Description of dummy task. It's tree fiddy on completion.", "NEW", null, 3.50);
        newTaskRef.setValue(dummyTask);
    }
}
