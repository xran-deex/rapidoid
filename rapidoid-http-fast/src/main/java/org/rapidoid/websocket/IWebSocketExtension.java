package org.rapidoid.websocket;

/**
 * Created by randy on 8/22/16.
 */
public interface IWebSocketExtension {
    byte[] incoming(byte[] bytes);
    byte[] outgoing(byte[] bytes);
    IWebSocketExtension getExtension();
    String addExtensionParameter(String param);
}
