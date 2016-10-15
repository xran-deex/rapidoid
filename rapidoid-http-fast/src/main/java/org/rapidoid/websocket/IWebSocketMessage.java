package org.rapidoid.websocket;

/**
 * Created by randy on 8/20/16.
 */
public interface IWebSocketMessage {
    boolean isText();
    boolean isBinary();
    String getString();
    byte[] getBytes();
}
