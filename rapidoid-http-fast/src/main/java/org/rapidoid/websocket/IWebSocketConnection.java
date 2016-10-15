package org.rapidoid.websocket;

import java.util.Map;

/**
 * Created by randy on 8/20/16.
 */
public interface IWebSocketConnection {
    void sendString(String msg);
    void sendJSON(Object msg);
    void sendError(short erroCode, String description);
    void sendClose();
    void broadcast(Object msg, boolean ignoreSender);
    void onMessage(IReceiveMessage receiver);
    void onClose(IConnectionClose closer);
    void sendBytes(byte[] bytes);
    Object session(String name);
    Map<String, Object> session();
    void session(String name, Object value);
    Long id();
}
