package org.rapidoid.websocket;

import org.rapidoid.data.JSON;
import org.rapidoid.http.Req;
import org.rapidoid.net.abstracts.Channel;
import org.rapidoid.net.impl.RapidoidConnection;

import java.io.UnsupportedEncodingException;
import java.util.*;

/**
 * Created by randy on 7/19/16.
 */
public class WebSocketConnection implements IWebSocketConnection {

    private WebSocketProtocol webSocketProtocol;
    private RapidoidConnection connection;
    private WebSocketHandler handler;
    private Queue<WebSocketMessage> messages;
    private Queue<WebSocketFrame> frameQueue;
    private WebSocketFrame incompleteFrame;
    private IReceiveMessage receiver;
    private IConnectionClose closer;
    private Req req;
    private List<IWebSocketExtension> extensions;
    private Map<String, Object> session;

    public WebSocketConnection(WebSocketProtocol proto, RapidoidConnection connection, WebSocketHandler handler, Req req, List<IWebSocketExtension> exts){
        this.webSocketProtocol = proto;
        this.connection = connection;
        this.handler = handler;
        this.req = req;
        this.messages = new ArrayDeque<WebSocketMessage>();
        this.frameQueue = new ArrayDeque<WebSocketFrame>();
        this.extensions = new ArrayList<IWebSocketExtension>();
        for (IWebSocketExtension ext: exts) {
            this.extensions.add(ext.getExtension());
        }
        session = new HashMap<String, Object>();
    }

    public List<IWebSocketExtension> getExtensions() {
        return extensions;
    }

    public WebSocketHandler getHandler() {
        return handler;
    }

    public void setIncompleteFrame(WebSocketFrame f){
        this.incompleteFrame = f;
    }

    public WebSocketFrame getIncompleteFrame(){ return incompleteFrame; }

    public Queue<WebSocketMessage> getMessages() { return messages; }

    public Queue<WebSocketFrame> getFrameQueue() { return frameQueue; }

    public Channel getChannel(){
        return connection;
    }

    public void sendPing(){
        Helper.sendPing(connection);
    }

    public void sendString(String msg){
        if(!extensions.isEmpty()){
            byte[] bytes;
            try {
                bytes = msg.getBytes("UTF-8");
                for (IWebSocketExtension ext : extensions) {
                    bytes = ext.outgoing(bytes);
                }
                Helper.sendCompressed(connection, bytes, MessageType.TEXT);
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
            return;
        }
        Helper.sendString(connection, msg);
    }

    public void sendBytes(byte[] b){
        if(!extensions.isEmpty()){
            byte[] bytes;
            bytes = b;
            for (IWebSocketExtension ext : extensions) {
                bytes = ext.outgoing(bytes);
            }
            Helper.sendCompressed(connection, bytes, MessageType.BINARY);
            return;
        }
        Helper.sendBytes(connection, b);
    }

    @Override
    public Object session(String name) {
        return session.get(name);
    }

    @Override
    public Map<String, Object> session() {
        return session;
    }

    @Override
    public void session(String name, Object value) {
        session.put(name, value);
    }

    @Override
    public Long id() {
        return connection.connId();
    }

    public void sendJSON(Object msg){
        if(!extensions.isEmpty()){
            byte[] bytes;
            bytes = JSON.stringifyToBytes(msg);
            for (IWebSocketExtension ext : extensions) {
                bytes = ext.outgoing(bytes);
            }
            Helper.sendCompressed(connection, bytes, MessageType.TEXT);
            return;
        }
        Helper.sendJSON(connection, msg);
    }

    public void broadcast(Object msg, boolean ignoreSender){
        for (WebSocketConnection conn : webSocketProtocol.getConnections().values()) {
            if(conn == this && !ignoreSender)
                continue;
            if(msg instanceof byte[]) {
                conn.sendBytes((byte[]) msg);
            } else if (msg instanceof String){
                conn.sendString((String)msg);
            } else {
                conn.sendJSON(msg);
            }
        }
    }

    public void sendError(short code, String desc){
        Helper.sendError(connection, new WebSocketStatusCode(code, desc));
    }

    public void sendClose(){
        Helper.sendClose(connection);
    }

    public void onMessage(IReceiveMessage receiveMessage){
        receiver = receiveMessage;
    }

    public void executeOnMessage(IWebSocketMessage msg){
        receiver.onMessage(msg);
    }

    public void onClose(IConnectionClose closer){
        this.closer = closer;
    }

    public void executeOnClose(){
        this.closer.onClose();
    }
}

