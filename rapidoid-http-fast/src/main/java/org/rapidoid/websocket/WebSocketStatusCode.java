package org.rapidoid.websocket;

/**
 * Created by randy on 7/29/16.
 */
public class WebSocketStatusCode {
    public static final short NORMAL_CLOSURE = 1000;
    public static final short GOING_AWAY = 1001;
    public static final short PROTOCOL_ERROR = 1002;
    public static final short UNKNOWN_DATA_TYPE = 1003;
    public static final short INCONSISTENT_MESSAGE_TYPE = 1007;
    public static final short POLICY_VIOLATION = 1008;
    public static final short MESSAGE_TOO_BIG = 1009;
    public static final short UNEXPECTED_CONDITION = 1011;
    public static final short NO_ERROR = -1;
    public static final short NEED_MORE_BYTES = -2;
    public static final short HAS_ERROR = -3;
    public static final short NOT_WEBSOCKET = -4;

    public short code;
    public String description;
    public WebSocketStatusCode(short code, String description){
        this.code = code;
        this.description = description;
    }
    public WebSocketStatusCode(short code){
        this.code = code;
        this.description = "";
    }
    public WebSocketStatusCode() {
        this.code = NO_ERROR;
        this.description = "";
    }
    public short getStatusCode(){
        return code;
    }
    public String getDescription(){
        return description;
    }
    public void setStatusCode(short code){
        this.code = code;
    }
    public void setDescription(String desc){
        this.description = desc;
    }
}
