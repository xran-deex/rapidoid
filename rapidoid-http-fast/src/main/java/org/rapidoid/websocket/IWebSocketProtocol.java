package org.rapidoid.websocket;

import java.util.List;

/**
 * Created by randy on 9/2/16.
 */
public interface IWebSocketProtocol {
    List<IWebSocketConnection> conns();
}
