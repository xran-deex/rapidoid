package org.rapidoid.websocket;

import org.rapidoid.log.Log;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

import static org.rapidoid.websocket.MessageType.CLOSE;
import static org.rapidoid.websocket.MessageType.PING;
import static org.rapidoid.websocket.MessageType.PONG;

/**
 * Created by randy on 7/19/16.
 */
public class WebSocketMessage implements IWebSocketMessage {
    private byte[] message;
    private List<WebSocketFrame> frames;
    public WebSocketMessage() {
        frames = new ArrayList<WebSocketFrame>();
    }

    public void addFrame(WebSocketFrame frame){
        frames.add(frame);
    }

    public void reset(){
        frames.clear();
    }

    public WebSocketStatusCode constructMsg(List<IWebSocketExtension> extensions) {
        int totalMessageLength = 0;
        for (WebSocketFrame frame : frames){
            totalMessageLength += frame.getLength();
        }
        byte[] bytes = new byte[totalMessageLength];
        int position = 0;

        for(WebSocketFrame frame : frames){
            if(frame.getMessage() == null) {
                Log.debug("Frame bytes are NULL!!!!");
            }
            System.arraycopy(frame.getMessage(), 0, bytes, position, frame.getLength());
            position += frame.getLength();
        }
        if(!extensions.isEmpty() && !isControl()){
            for (IWebSocketExtension ext : extensions) {
                bytes = ext.incoming(bytes);
            }
        }
        if(bytes.length < 512) {
            if(getType() == MessageType.TEXT){
                ByteBuffer b = ByteBuffer.wrap(bytes, 0, bytes.length);
                if(!WebSocketFrame.isValidUTF8(b)){
                    Log.debug("Invalid utf8");
                    return new WebSocketStatusCode(WebSocketStatusCode.INCONSISTENT_MESSAGE_TYPE, "Invalid UTF8");
                }
            }
            if (getType() == CLOSE) {
                if(bytes.length > 2){
                    ByteBuffer b = ByteBuffer.wrap(bytes, 2, bytes.length - 2);
                    if(!WebSocketFrame.isValidUTF8(b)){
                        Log.debug("Invalid utf8");
                        return new WebSocketStatusCode(WebSocketStatusCode.INCONSISTENT_MESSAGE_TYPE, "Invalid UTF8");
                    }
                }
            }
        }
        message = bytes;
        return new WebSocketStatusCode(WebSocketStatusCode.NO_ERROR);
    }

    public byte[] getMsg(){
        return message;
    }

    @Override
    public boolean isText() {
        return getType() == MessageType.TEXT;
    }

    @Override
    public boolean isBinary() {
        return getType() == MessageType.BINARY;
    }

    public boolean isControl() {
        MessageType type = getType();
        return type == CLOSE || type == PING || type == PONG;
    }

    public String getString() {
        return new String(getMsg());
    }

    @Override
    public byte[] getBytes() {
        return getMsg();
    }

    public MessageType getType(){
        return frames.size() > 0 ? frames.get(0).getType() : null;
    }
}
