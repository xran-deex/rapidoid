package org.rapidoid.websocket;

/**
 * Created by randy on 8/20/16.
 */
public interface IReceiveMessage {
    void onMessage(IWebSocketMessage msg);
}
