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

import org.json.simple.JSONObject;
import org.json.simple.JSONValue;
import org.json.simple.parser.ParseException;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import javax.net.ssl.SSLSocketFactory;
import java.io.IOException;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

// Sample Smack implementation of a client for FCM Cloud Connection Server. Most of it has been taken more or less

public class FcmServer implements StanzaListener {

    private static final Logger logger = Logger.getLogger(FcmServer.class.getName());
    //private static final String TAG = FCM.FcmServer.class.getSimpleName();

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
     */
    @SuppressWarnings("unchecked")
    @Override
    public void processStanza(Stanza packet) {
        GcmPacketExtension gcmPacket = (GcmPacketExtension) packet.getExtension(FCM_NAMESPACE);
        String json = gcmPacket.getJson();
        try {
            Map<String, Object> jsonMap = (Map<String, Object>) JSONValue.parseWithException(json);
            Object messageType = jsonMap.get("message_type");
            //logger.log(Level.INFO, "Message: " + json);
            if (messageType == null) {

                logger.log(Level.INFO, "Received Custom Message.");
                //todo: if has key (messageType) then send ack
                String customMessageType = (String) ((JSONObject) (jsonMap.getOrDefault("data", ""))).getOrDefault("messageType", "");
                if (Objects.equals(customMessageType, "reply-test")) {
                    handleReplyTest(jsonMap);
                } else if (Objects.equals(customMessageType, "new-task-reply")) {
                    handleNewTaskReply(jsonMap);
                } else {
                    UpstreamMessage upstreamMessage = MessageHelper.createUpstreamMessage(jsonMap);
                    handleUpstreamMessage(upstreamMessage); // normal upstream message
                }
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
            //logger.log(Level.INFO, "Error parsing JSON: " + json, e.getMessage());
        }
    }

    /**
     * Handles an upstream message from a device client through FCM
     */
    private void handleUpstreamMessage(UpstreamMessage upstreamMessage) {
        String ack = MessageHelper.createJsonAck(upstreamMessage.getFrom(), upstreamMessage.getMessageId());
        logger.log(Level.INFO, "Sending norm ack.");
        send(ack);
    }


    private void handleReplyTest(Map<String, Object> jsonMap) {
        UpstreamMessage upstreamMessage = MessageHelper.createUpstreamMessage(jsonMap);

        // Send ACK to FCM
        String ack = MessageHelper.createJsonAck(upstreamMessage.getFrom(), upstreamMessage.getMessageId());
        logger.log(Level.INFO, "Sending reply test ack.");
        send(ack);

        String count = upstreamMessage.getDataPayload().getOrDefault("count", "-1");
        int countVal = Integer.parseInt(count) + 1;

        // Send a reply downstream message to a device
        Map<String, String> dataPayload = new HashMap<String, String>();
        dataPayload.put("messageType", "reply-test");
        dataPayload.put("count", String.valueOf(countVal));
        DownstreamMessage message = new DownstreamMessage(upstreamMessage.getFrom(), upstreamMessage.getMessageId(), dataPayload);
//		message.setTimeToLive(3);
//		message.setDeliveryReceiptRequested(false);
//		message.setContentAvailable(true);
//		message.setNotificationPayload(null);

        String jsonRequest = MessageHelper.createJsonDownstreamMessage(message);

        logger.log(Level.INFO, "Sending reply.");
        send(jsonRequest);
    }

    //Parameter String toId specifies who the message is sent to, can be a topics path or client device ID
    public void sendTaskData(String jsonStringTask, String taskId, String toId) {
        String messageId = getUniqueMessageId();
        Map<String, String> dataPayload = new HashMap<String, String>();
        dataPayload.put("messageType", "new-task");
        dataPayload.put("task", jsonStringTask);
        dataPayload.put("taskId", taskId);
        DownstreamMessage message = new DownstreamMessage(toId, messageId, dataPayload);
        message.setTimeToLive(60);
        String jsonRequest = MessageHelper.createJsonDownstreamMessage(message);
        send(jsonRequest);
    }

    private void handleNewTaskReply(Map<String, Object> jsonMap) {
        UpstreamMessage upstreamMessage = MessageHelper.createUpstreamMessage(jsonMap);

        // Send ACK to FCM
        String ack = MessageHelper.createJsonAck(upstreamMessage.getFrom(), upstreamMessage.getMessageId());
        logger.log(Level.INFO, "Sending new task reply ack.");
        send(ack);

        //Store response
        Integer locationScore = Integer.valueOf(upstreamMessage.getDataPayload().get("locationScore"));
        String clientId = upstreamMessage.getFrom();
        newTaskReplyData.addData(clientId, locationScore);
    }

    public void sendTaskNotificationToTopUsers(String jsonTaskString, String title) {
        //sendTaskNotification(10, jsonTaskString, title);
    }

    public void sendTaskNotificationToTopUsers(int amount, String jsonTaskString, String title) {
        List<String> clientList = newTaskReplyData.getTopUsers(amount);
        // Send a reply downstream message to a device
        Map<String, String> dataPayload = new HashMap<String, String>();
        dataPayload.put("messageType", "new-task-notification");
        dataPayload.put("taskData", jsonTaskString);
        Map<String, String> notificationPayload = new HashMap<String, String>();
        notificationPayload.put("title", title);
        notificationPayload.put("body", "New Task");
        notificationPayload.put("test", "find.me");

        //Map<String, Object> map = MessageHelper.createAttributeMap(downstreamMessage);
        for (String clientId : clientList) {
            DownstreamMessage message = new DownstreamMessage(clientId, getUniqueMessageId(), dataPayload);
            message.setNotificationPayload(notificationPayload);
            String jsonRequest = MessageHelper.createJsonDownstreamMessage(message);
            send(jsonRequest);
        }

        logger.log(Level.INFO, "Sent task to " + clientList.size() + " clients.");
    }

    public void sendTaskIdForNotification(String taskId, String toId) {
        String messageId = getUniqueMessageId();
        Map<String, String> dataPayload = new HashMap<String, String>();
        dataPayload.put("messageType", "task-for-notification");
        dataPayload.put("taskId", taskId);
        DownstreamMessage message = new DownstreamMessage(toId, messageId, dataPayload);
        message.setTimeToLive(60);
        String jsonRequest = MessageHelper.createJsonDownstreamMessage(message);
        send(jsonRequest);
    }

    public void sendTaskNotification(String taskId, String toId, String jsonTaskString, String title) {
        // Send a reply downstream message to a device
        Map<String, String> dataPayload = new HashMap<String, String>();
        dataPayload.put("messageType", "new-task-notification");
        dataPayload.put("taskData", jsonTaskString);
        dataPayload.put("taskId", taskId);
        Map<String, String> notificationPayload = new HashMap<String, String>();
        notificationPayload.put("title", title);
        notificationPayload.put("body", "New Task");

        //Map<String, Object> map = MessageHelper.createAttributeMap(downstreamMessage);
        DownstreamMessage message = new DownstreamMessage(toId, getUniqueMessageId(), dataPayload);
        message.setNotificationPayload(notificationPayload);
        String jsonRequest = MessageHelper.createJsonDownstreamMessage(message);
        send(jsonRequest);

        logger.log(Level.INFO, "Sent task notification.");
    }


    public void sendPingHack(String toId) {
        String messageId = getUniqueMessageId();
        DownstreamMessage message = new DownstreamMessage(toId, messageId, null);
        message.setTimeToLive(10);
        String jsonRequest = MessageHelper.createJsonDownstreamMessage(message);
        send(jsonRequest);
    }


    /**
     * Handles a NACK message from FCM
     */
    private void handleNackReceipt(Map<String, Object> jsonMap) {
        String errorCode = (String) jsonMap.get("error");
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
        String controlType = (String) jsonMap.get("control_type");
        if (controlType != null) {
            logger.log(Level.INFO, "Received FCM Control Message: " + controlType + ",  Message: " + jsonMap);
        }
    }

    /**
     * Sends a downstream message to FCM
     */
    public void send(String jsonRequest) {
        // TODO: Resend the message using exponential back-off
        Stanza request = new GcmPacketExtension(jsonRequest).toPacket();
        try {
            connection.sendStanza(request);
        } catch (NotConnectedException | InterruptedException e) {
            logger.log(Level.WARNING, "The packet could not be sent due to a connection problem: " + request.toXML());
        }
    }


    public void sendMessageToRecipientList(DownstreamMessage downstreamMessage, List<String> recipients) {
        Map<String, Object> map = MessageHelper.createAttributeMap(downstreamMessage);
        for (String toRegId : recipients) {
            String messageId = getUniqueMessageId();
            map.put("message_id", messageId);
            map.put("to", toRegId);
            String jsonRequest = MessageHelper.createJsonMessage(map);
            send(jsonRequest);
        }
    }

    /**
     * Returns a random message id to uniquely identify a message
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
