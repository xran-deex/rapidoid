package org.rapidoid.websocket;

import org.rapidoid.http.Req;

/**
 * Created by randy on 8/20/16.
 */
public interface IWebSocketRequest {
    void onRequest(IWebSocketProtocol ws, Req req, IWebSocketConnection conn);
}
