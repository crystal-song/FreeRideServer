package FCM;


import org.json.simple.JSONValue;

import java.util.HashMap;
import java.util.Map;

public class MessageBuilder {

    private boolean prepared;
    private Map<String, Object> map;
    private Map<String, String> dataPayload;
    private Map<String, String> notificationPayload;

    public MessageBuilder() {
        map = new HashMap<>();
        prepared = true;
    }

    public MessageBuilder addToDataPayload(String key, String value) {
        if (dataPayload == null){
            dataPayload = new HashMap<>();
        }
        dataPayload.put(key, value);
        prepared = false;
        return this;
    }

    public MessageBuilder addToNotificationPayload(String key, String value) {
        if (notificationPayload == null){
            notificationPayload = new HashMap<>();
        }
        notificationPayload.put(key, value);
        prepared = false;
        return this;
    }

    public MessageBuilder messageId(String id) {
        map.put("message_id", id);
        return this;
    }

    public MessageBuilder messageType(String type) {
        map.put("message_type", type);
        return this;
    }

    public MessageBuilder to(String to) {
        map.put("to", to);
        return this;
    }

    public MessageBuilder timeToLive(int seconds) {
        map.put("time_to_live", seconds);
        return this;
    }

    public MessageBuilder highPriority() {
        map.put("priority", "high");
        return this;
    }

    public MessageBuilder normalPriority() {
        map.put("priority", "normal");
        return this;
    }

    public MessageBuilder prepare() {
        if (dataPayload != null) {
            map.put("data", dataPayload);
        }
        if (notificationPayload != null) {
            map.put("notification", notificationPayload);
        }
        return this;
    }

    public String build() {
        //check that map has 'to' and 'messageId'
        if(!prepared) {
            prepare();
        }
        if (dataPayload != null) {
            map.put("data", dataPayload);
        }
        if (notificationPayload != null) {
            map.put("notification", notificationPayload);
        }
        return JSONValue.toJSONString(map);
    }

    //todo priority and ?
}
