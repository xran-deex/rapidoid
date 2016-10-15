package org.rapidoid.websocket;

import org.rapidoid.buffer.Buf;
import org.rapidoid.commons.Str;
import org.rapidoid.http.Req;
import org.rapidoid.log.Log;
import org.rapidoid.net.abstracts.Channel;
import org.rapidoid.net.impl.RapidoidConnection;
import org.rapidoid.util.Constants;

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;

import static org.rapidoid.websocket.WebSocketStatusCode.*;

/**
 * Created by randy on 7/19/16.
 */
public class WebSocketProtocol implements IWebSocketProtocol {

    private static String MAGIC_KEY = "258EAFA5-E914-47DA-95CA-C5AB0DC85B11";

    private static boolean IS_NOT_WEBSOCKET = false;
    private static boolean IS_WEBSOCKET = true;

    private Map<Long, WebSocketConnection> connections;
    private List<IWebSocketConnection> conns;
    private Map<Long, WebSocketConnection> pings;

    private List<IWebSocketExtension> extensions = new ArrayList<IWebSocketExtension>();

    private Timer pinger;

    private static final int PING_INTERVAL = 20;
    private static final int CLEANUP_INTERVAL = 30; // clean up sockets that haven't sent a pong in 30 sec

    public WebSocketProtocol(){
        connections = new HashMap<Long, WebSocketConnection>();
        pings = new HashMap<Long, WebSocketConnection>();
        conns = new ArrayList<IWebSocketConnection>();
    }

    public Map<Long, WebSocketConnection> getConnections() {
        return connections;
    }

    public List<IWebSocketConnection> conns() {
        return conns;
    }

    public void addExtension(IWebSocketExtension ext){
        extensions.add(ext);
    }

    public byte[] UpgradeConnection(String key, Req req) {
        MessageDigest sha1 = null;
        try {

            sha1 = MessageDigest.getInstance("SHA-1");

        } catch (NoSuchAlgorithmException e) {
        }

        sha1.reset();

        try {
            String combined = key + MAGIC_KEY;
            sha1.update(combined.getBytes("UTF-8"));

        } catch (UnsupportedEncodingException e) {
        }

        byte[] digest = sha1.digest();
        String base64 = Str.toBase64(digest);

        StringBuilder response = new StringBuilder();
        response.append("HTTP/1.1 101 Switching Protocols\r\n");
        response.append("Connection: Upgrade\r\n");
        response.append("Upgrade: websocket\r\n");
        if(!extensions.isEmpty()) {
            String header = req.header("Sec-WebSocket-Extensions", null);
            if(header != null) {
                response.append("Sec-WebSocket-Extensions: ");
                String[] exts = header.split(",");
                int extIndex = 0;
                StringBuilder responseParams = new StringBuilder();
                for (String ext : exts) {
                    ext = ext.trim();
                    IWebSocketExtension extension = extensions.get(extIndex++);
                    String[] params = ext.split(";");

                    int paramsIdx = 0;
                    for (String param : params) {
                        param = param.trim();
                        if(paramsIdx == 0){
                            response.append(param); // first will be the extension;
                            paramsIdx++;
                            if(params.length > 1){
                                response.append("; ");
                            }
                            continue;
                        }
                        paramsIdx++;
                        String supportedExtension = extension.addExtensionParameter(param);
                        if (supportedExtension == null) continue;
                        responseParams.append(supportedExtension);
                        if(paramsIdx < params.length - 1)
                            responseParams.append("; ");
                    }
                    if(extIndex < exts.length - 1){
                        responseParams.append(", ");
                    }
                }
                response.append(responseParams.subSequence(0, responseParams.length())); // remove last ;
                response.append("\r\n");
            }
        }
        response.append("Sec-WebSocket-Accept: ");
        response.append(base64);
        Log.debug(response.toString());

        return response.toString().getBytes();
    }

    public boolean CheckForWebSocket(Buf buf, Channel channel){
        if(!connections.containsKey(channel.connId())) {
            return IS_NOT_WEBSOCKET;
        }

        WebSocketConnection conn = connections.get(channel.connId());
        Queue<WebSocketMessage> msgs = conn.getMessages();
        Queue<WebSocketFrame> queue = conn.getFrameQueue();

        assert msgs.isEmpty();
        WebSocketStatusCode code;
        WebSocketFrame frame = conn.getIncompleteFrame();
        if(frame == null){
            conn.setIncompleteFrame(new WebSocketFrame());
            frame = conn.getIncompleteFrame();
        }

        do {

            code = frame.process(buf, !conn.getExtensions().isEmpty());

            if(code.getStatusCode() != NO_ERROR && code.getStatusCode() != NEED_MORE_BYTES){
                // error
                handleError(conn, code);
                return IS_WEBSOCKET;
            }

            if(frame.isComplete()){
                if(frame.hasError()){
                    handleError(conn, frame.getStatusCode());
                    return IS_WEBSOCKET;
                }
                if(frame.isContinuation()){
                    if(queue.isEmpty()){
                        code.setDescription("Missing start fragment");
                        code.setStatusCode(PROTOCOL_ERROR);
                        handleError(conn, code);
                        return IS_WEBSOCKET;
                    }
                }

                if(!frame.isFinished()){
                    if(frame.isControlFrame()){
                        // control frames can't be fragmented
                        code.setStatusCode(PROTOCOL_ERROR);
                        code.setDescription("Control frames cannot be fragmented");
                        handleError(conn, code);
                        return IS_WEBSOCKET;
                    }
                    if(!frame.isContinuation()){
                        if(!queue.isEmpty()){
                            code.setStatusCode(PROTOCOL_ERROR);
                            code.setDescription("Expected continuation frames");
                            handleError(conn, code);
                            return IS_WEBSOCKET;
                        }
                    }
                    queue.add(frame);
                }
                if(frame.isControlFrame()){
                    WebSocketMessage msg = new WebSocketMessage();
                    msg.addFrame(frame);
                    WebSocketStatusCode msgCode = msg.constructMsg(conn.getExtensions());

                    if(msgCode.getStatusCode() != NO_ERROR){
                        handleError(conn, msgCode);
                        return IS_WEBSOCKET;
                    }
                    conn.setIncompleteFrame(null);
                    switch(frame.getType()){
                        case CLOSE:
                            System.out.println(frame.getCloseMessage());
                            removeConnection(conn.getChannel());
                            Helper.sendClose(conn.getChannel());
                            break;
                        case PING:
                            Helper.sendPong(conn.getChannel(), msg.getMsg());
                            break;
                        case PONG:
                            handlePong(conn.getChannel());
                            break;
                    }
                } else if(frame.isFinished()){

                    if(!frame.isContinuation() && !queue.isEmpty()){
                        code.setDescription("Expected continuation frame");
                        code.setStatusCode(PROTOCOL_ERROR);
                        handleError(conn, code);
                        return IS_WEBSOCKET;
                    }

                    WebSocketMessage msg = new WebSocketMessage();

                    // add all queued frames to the message
                    while(!queue.isEmpty()){
                        msg.addFrame(queue.remove());
                    }
                    msg.addFrame(frame); // make sure to add this frame
                    WebSocketStatusCode msgCode = msg.constructMsg(conn.getExtensions());

                    if(msgCode.getStatusCode() != NO_ERROR){
                        handleError(conn, msgCode);
                        return IS_WEBSOCKET;
                    }

                    switch(msg.getType()){
                        case BINARY:
                        case TEXT:
                            try {
                                conn.executeOnMessage(msg);
                            } catch (Exception e) {
                                conn.sendError(PROTOCOL_ERROR, "Error processing message");
                            }
                            break;
                    }
                    conn.setIncompleteFrame(null);
                }
                frame = new WebSocketFrame();
                conn.setIncompleteFrame(frame);
            }
        } while (code.getStatusCode() == NEED_MORE_BYTES);
        Log.debug("Loop end");
        return IS_WEBSOCKET;
    }

    public void handlePong(Channel channel){
        WebSocketConnection removed = pings.remove(channel.connId());
        if(removed == null){
            // this pong was unsolicated... ignore...
            Log.debug("Received unsolicated pong");
        }
    }

    public void handleError(WebSocketConnection conn, WebSocketStatusCode statusCode){
        removeConnection(conn.getChannel());
        Helper.sendError(conn.getChannel(), statusCode);
    }

    public void removeConnection(Channel channel){
        WebSocketConnection conn = null;
        if(connections.containsKey(channel.connId())){
            conn = connections.remove(channel.connId());
            Log.debug("Removed connection. Size: " + connections.size());
            conns.remove(conn);
        }
        if(conn == null) return;
        conn.executeOnClose();
        if(connections.isEmpty() && pinger != null){
            pinger.cancel();
        }
    }

    public void acceptConnection(Channel channel, String key, WebSocketHandler handler, Req req){
        // add the connection to the list of connections
        WebSocketConnection conn = new WebSocketConnection(this, (RapidoidConnection)channel, handler, req, extensions);
        if(!connections.containsKey(channel.connId())){
            connections.put(channel.connId(), conn);
            Log.debug("Accepted connection. Size: " + connections.size());
            conns.add(conn);
        }
        if(connections.size() == 1){
            pinger = setupPinger(PING_INTERVAL);
        }
        channel.write(UpgradeConnection(key, req));
        channel.write(Constants.CR_LF_CR_LF);
        channel.send();
        handler.handleRequest(this, req, conn);
    }

    // start a timer to send pings to all clients
    private  Timer setupPinger(int seconds){
        Timer timer = new Timer();
        timer.scheduleAtFixedRate(new RemindTask(), 1000, seconds * 1000);
        return timer;
    }

    class CleanupTask extends TimerTask {

        @Override
        public void run() {
            for (Map.Entry<Long, WebSocketConnection> entry: pings.entrySet()) {
                WebSocketConnection pingNotReceived = entry.getValue();
                Long id = entry.getKey();
                Log.debug("Removing client "+ id +"...");
                removeConnection(pingNotReceived.getChannel());
            }
            pings.clear();
        }
    }

    class RemindTask extends TimerTask {
        public RemindTask(){
        }
        public void run() {
            for (Map.Entry<Long, WebSocketConnection> entry: connections.entrySet()) {
                WebSocketConnection connection = entry.getValue();
                Long id = entry.getKey();
                Log.debug("Sending ping to client "+ id +"...");
                connection.sendPing();
                pings.put(id, connection);
            }
            Timer pingTimeout = new Timer();
            pingTimeout.schedule(new CleanupTask(), CLEANUP_INTERVAL * 1000);
        }
    }
}
