package FCM;

import org.jivesoftware.smack.ConnectionConfiguration;
import org.jivesoftware.smack.ReconnectionManager;
import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.SmackException.NotConnectedException;
import org.jivesoftware.smack.StanzaListener;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.filter.StanzaFilter;
import org.jivesoftware.smack.packet.ExtensionElement;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.packet.Stanza;
import org.jivesoftware.smack.provider.ExtensionElementProvider;
import org.jivesoftware.smack.provider.ProviderManager;
import org.jivesoftware.smack.roster.Roster;
import org.jivesoftware.smack.tcp.XMPPTCPConnection;
import org.jivesoftware.smack.tcp.XMPPTCPConnectionConfiguration;
import org.jivesoftware.smackx.ping.PingManager;
import org.json.simple.JSONValue;
import org.json.simple.parser.ParseException;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import javax.net.ssl.SSLSocketFactory;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

// Sample Smack implementation of a client for FCM Cloud Connection Server. Most of it has been taken more or less

public class FcmServer implements StanzaListener {

    private static final Logger logger = Logger.getLogger(FcmServer.class.getName());

    private static final String FCM_SERVER = "fcm-xmpp.googleapis.com";
    private static final int FCM_PORT = 5236;
    private static final String FCM_ELEMENT_NAME = "gcm";
    private static final String FCM_NAMESPACE = "google:mobile:data";
    private static final String FCM_SERVER_CONNECTION = "gcm.googleapis.com";

    private static FcmServer sInstance = null;
    private XMPPTCPConnection connection;
    private String mApiKey = null;
    private boolean mDebuggable = false;
    private String fcmServerUsername = null;

    private NewTaskReplyData newTaskReplyData;

    public static FcmServer getInstance() {
        if (sInstance == null) {
            throw new IllegalStateException("You have to prepare the client first");
        }
        return sInstance;
    }

    public static FcmServer prepareClient(String projectId, String apiKey, boolean debuggable) {
        synchronized (FcmServer.class) {
            if (sInstance == null) {
                sInstance = new FcmServer(projectId, apiKey, debuggable);
            }
        }
        return sInstance;
    }

    private FcmServer(String projectId, String apiKey, boolean debuggable) {
        this();
        mApiKey = apiKey;
        mDebuggable = debuggable;
        fcmServerUsername = projectId + "@" + FCM_SERVER_CONNECTION;
        newTaskReplyData = new NewTaskReplyData();
    }

    private FcmServer() {
        // Add GcmPacketExtension
        ProviderManager.addExtensionProvider(FCM_ELEMENT_NAME, FCM_NAMESPACE,
                new ExtensionElementProvider<GcmPacketExtension>() {
                    @Override
                    public GcmPacketExtension parse(XmlPullParser parser, int initialDepth)
                            throws XmlPullParserException, IOException, SmackException {
                        String json = parser.nextText();
                        return new GcmPacketExtension(json);
                    }
                });
    }

    /**
     * Connects to FCM Cloud Connection Server using the supplied credentials
     */
    public void connect() throws XMPPException, SmackException, IOException, InterruptedException {
        XMPPTCPConnection.setUseStreamManagementResumptionDefault(true);
        XMPPTCPConnection.setUseStreamManagementDefault(true);

        XMPPTCPConnectionConfiguration.Builder config = XMPPTCPConnectionConfiguration.builder();
        config.setXmppDomain("FCM XMPP Client Connection Server");
        config.setHost(FCM_SERVER);
        config.setPort(FCM_PORT);
        config.setSecurityMode(ConnectionConfiguration.SecurityMode.ifpossible);
        config.setSendPresence(false);
        config.setSocketFactory(SSLSocketFactory.getDefault());
        // Launch a window with info about packets sent and received
        config.setDebuggerEnabled(mDebuggable);

        // Create the connection
        connection = new XMPPTCPConnection(config.build());

        // Connect
        connection.connect();

        // Enable automatic reconnection
        ReconnectionManager.getInstanceFor(connection)
                .enableAutomaticReconnection();

        // Disable Roster at login
        Roster.getInstanceFor(connection).setRosterLoadedAtLogin(false);


        // Handle incoming packets (the class implements the StanzaListener)
        connection.addAsyncStanzaListener(this, new StanzaFilter() {
            @Override
            public boolean accept(Stanza stanza) {
                return stanza.hasExtension(FCM_ELEMENT_NAME, FCM_NAMESPACE);
            }
        });


        // Set the ping interval
        final PingManager pingManager = PingManager.getInstanceFor(connection);
        pingManager.setPingInterval(100);
        pingManager.registerPingFailedListener(() -> {
            //logger.info("The ping failed, restarting the ping interval again ...");
            pingManager.setPingInterval(100);
        });

        connection.login(fcmServerUsername, mApiKey);
        logger.log(Level.INFO, "Logged in: " + fcmServerUsername);
    }

    /**
     * Handles incoming messages
     * This project has stopped using upstream messages to communicate with the client
     */
    @SuppressWarnings("unchecked")
    @Override
    public void processStanza(Stanza packet) {
        GcmPacketExtension gcmPacket = (GcmPacketExtension) packet.getExtension(FCM_NAMESPACE);
        String json = gcmPacket.getJson();
        try {
            Map<String, Object> jsonMap = (Map<String, Object>) JSONValue.parseWithException(json);
            Object messageType = jsonMap.get("message_type");
            if (messageType == null) {

                String from = jsonMap.get("from").toString();
                String messageId = jsonMap.get("message_id").toString();

                // Send ACK to FCM
                sendAck(from, messageId);

            } else {
                switch (messageType.toString()) {
                    //'ack', 'nack', 'receipt', 'control'
                    case "nack" :
                        handleNackReceipt(jsonMap);
                        break;
                    case "control":
                        handleControlMessage(jsonMap);
                        break;
                }
            }
        } catch (ParseException e) {
            logger.log(Level.INFO, "Error parsing JSON: " + json, e.getMessage());
        }
    }

    //Parameter String toId specifies who the message is sent to, can be a topics path or client device ID

    /**
     * Sends task data as a JSON string to client or topic.
     * task data is added to data payload of outgoing message.
     * @param jsonStringTask task data
     * @param taskId of task
     * @param toId client token or topic name
     */
    public void sendTaskData(String jsonStringTask, String taskId, String toId) {

        String message = new MessageBuilder()
                .to(toId)
                .messageId(getUniqueMessageId())
                .addToDataPayload("messageType", "new-task")
                .addToDataPayload("task", jsonStringTask)
                .addToDataPayload("taskId", taskId)
                .timeToLive(60)
                .build();

        sendMessage(message);
    }

    /**
     * @param jsonTaskString task data
     * @param title of task
     */
    public void sendTaskNotificationToTopUsers(String jsonTaskString, String title) {
        //sendTaskNotificationToTopUsers(10, jsonTaskString, title);
    }

    /**
     * @param amount of users to send notification to
     * @param jsonTaskString task data
     * @param title of task
     */
    public void sendTaskNotificationToTopUsers(int amount, String jsonTaskString, String title) {
        List<String> clientList = newTaskReplyData.getTopUsers(amount);
        sendTaskNotification(clientList, jsonTaskString);

        logger.log(Level.INFO, "Sent task to " + clientList.size() + " clients.");
    }

    /**
     * Sends task data and notification payload to client, to display a notification for the user.
     * @param userIds user tokens or topic
     * @param jsonTaskString task data
     */
    public void sendTaskNotification(List<String> userIds, String jsonTaskString) {

        MessageBuilder messageBuilder = new MessageBuilder()
                .addToDataPayload("messageType", "new-task-notification")
                .addToDataPayload("taskData", jsonTaskString)
                .addToNotificationPayload("body", "New Task")
                .prepare();

        for (String userId : userIds) {
            String message = messageBuilder
                    .to(userId)
                    .messageId(getUniqueMessageId())
                    .build();
            sendMessage(message);
        }

        logger.log(Level.INFO, "Sent task notification to users.");
    }

    /**
     * Handles a NACK message from FCM
     */
    private void handleNackReceipt(Map<String, Object> jsonMap) {
        String errorCode = String.valueOf(jsonMap.get("error"));
        if (errorCode != null) {
            logger.log(Level.INFO, "Received NACK FCM Error Code: " + errorCode + ",  Message: " + jsonMap);
        }
        //NACK Types: INVALID_JSON, BAD_REGISTRATION, DEVICE_UNREGISTERED, BAD_ACK, SERVICE_UNAVAILABLE,
        //INTERNAL_SERVER_ERROR, DEVICE_MESSAGE_RATE_EXCEEDED, TOPICS_MESSAGE_RATE_EXCEEDED, CONNECTION_DRAINING"
    }

    /**
     * Handles a Control message from FCM
     */
    private void handleControlMessage(Map<String, Object> jsonMap) {
        String controlType = String.valueOf(jsonMap.get("control_type"));
        if (controlType != null) {
            logger.log(Level.INFO, "Received FCM Control Message: " + controlType + ",  Message: " + jsonMap);
        }
    }

    /**
     * Sends ACK to FCM
     */
    public void sendAck(String from, String messageId) {
        String ack = new MessageBuilder()
                .messageId(messageId)
                .to(from)
                .messageType("ack")
                .build();
        logger.log(Level.INFO, "Sending reply test ack.");
        sendMessage(ack);
    }

    /**
     * Sends a downstream message to FCM
     */
    public void sendMessage(String messageJson) {
        // TODO: Resend the message using exponential back-off
        Stanza request = new GcmPacketExtension(messageJson).toPacket();
        try {
            connection.sendStanza(request);
        } catch (NotConnectedException | InterruptedException e) {
            logger.log(Level.WARNING, "The packet could not be sent due to a connection problem: " + request.toXML());
        }
    }

    /**
     * Returns a random message Id to uniquely identify a message
     */
    public static String getUniqueMessageId() {
        return "msgId_" + UUID.randomUUID().toString();
    }

    /**
     * XMPP Packet Extension for GCM Cloud Connection Server
     */
    public class GcmPacketExtension implements ExtensionElement {

        private String json;

        public GcmPacketExtension(String json) {
            this.json = json;
        }

        public String getJson() {
            return json;
        }

        @Override
        public String toXML() {
            return String.format("<%s xmlns=\"%s\">%s</%s>",
                    FCM_ELEMENT_NAME, FCM_NAMESPACE, json, FCM_ELEMENT_NAME);
        }

        public Stanza toPacket() {
            Message message = new Message();
            message.addExtension(this);
            return message;
        }

        @Override
        public String getElementName() {
            return FCM_ELEMENT_NAME;
        }

        @Override
        public String getNamespace() {
            return FCM_NAMESPACE;
        }
    }
}
