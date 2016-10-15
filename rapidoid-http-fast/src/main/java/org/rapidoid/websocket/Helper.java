package org.rapidoid.websocket;

import org.rapidoid.data.JSON;
import org.rapidoid.log.Log;
import org.rapidoid.net.abstracts.Channel;

import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * Created by randy on 7/19/16.
 */
public class Helper {


    public static boolean isFin(byte b){
//        int val = b & 0x80;
//        val = val >> 4;
//        return val == 8;
        return isMasked(b); // both check for the first bit set
    }

    public static boolean isMasked(byte b){
        return (b & 0x80) == 0x80;
    }

    public static boolean reserved(byte b){
//        int val = b & 0x30;
//        val = val >> 4;
//        return val > 0 && val < 8;
        return rsv1(b) || rsv2(b) || rsv3(b);
    }

    public static boolean rsv1(byte b){
        int val = b & 0x40;
        val = val >> 4;
        return val == 4;
    }

    public static boolean rsv2(byte b){
        int val = b & 0x20;
        val = val >> 4;
        return val == 2;
    }

    public static boolean rsv3(byte b){
        int val = b & 0x10;
        val = val >> 4;
        return val == 1;
    }

    public static MessageType getMessageType(byte b){
        int first = b & 0x0f;
        MessageType type = null;
        switch (first){
            case 0:
                type = MessageType.CONTINUATION;
                Log.debug("Got a continuation msg.");
                break;
            case 1:
                type = MessageType.TEXT;
                Log.debug("Got a text msg.");
                break;
            case 2:
                type = MessageType.BINARY;
                Log.debug("Got a binary msg.");
                break;
            case 8:
                type = MessageType.CLOSE;
                Log.debug("Got a close msg.");
                break;
            case 9:
                type = MessageType.PING;
                Log.debug("Got a ping!");
                break;
            case 10:
                type =  MessageType.PONG;
                Log.debug("Got a pong!");
                break;
            default:
                type = MessageType.UNKNOWN;
                Log.error("Unknown message type.");
                break;
        }
        return type;
    }

    public static int getMessageLength(byte b){
        int len = (b & 0x7f);
        return len;
    }

    public static long parseLengthBytes(byte[] bytes){
        byte[] len_bytes;
        if (bytes.length == 2){
            // 2 bytes
            len_bytes = new byte[4];
            len_bytes[2] = bytes[0];
            len_bytes[3] = bytes[1];
            len_bytes[0]=  0;
            len_bytes[1] = 0;
        } else {
            // 8 bytes
            len_bytes = bytes;
        }
        ByteBuffer _bytes;
        if(len_bytes.length == 4) {
            _bytes = ByteBuffer.wrap(len_bytes);
            int result = _bytes.getInt();
            return result;
        }
        if(len_bytes.length == 8) {
            _bytes = ByteBuffer.wrap(len_bytes);
            return _bytes.getLong();
        }
        return 0;
    }

    public static void sendBytes(Channel ctx, byte[] msg){
        send(ctx, msg, MessageType.BINARY);
    }

    public static void sendString(Channel ctx, String msg){
        try {
            byte[] msgAsBytes = msg.getBytes("UTF-8");
            send(ctx, msgAsBytes, MessageType.TEXT);
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
    }

    public static void sendJSON(Channel ctx, Object msg){
        byte[] msgAsBytes = JSON.stringifyToBytes(msg);
        send(ctx, msgAsBytes, MessageType.TEXT);
    }

    private static void send(Channel ctx, byte[] msg, MessageType type){
        int msgLen = msg.length;
        byte first;
        if(type == MessageType.TEXT)
            first = (byte)0x81;
        else
            first = (byte)0x82;
        byte[] lengthBytes = null;

        if(msgLen > 125 && msgLen <= 0xffff) {
            // next 2 bytes are the length
            lengthBytes = new byte[3]; // 2 + 1
            lengthBytes[0] = 126; // this indicates to the client that the next 2 bytes are the length
            ByteBuffer bytes = ByteBuffer.allocate(2);
            byte[] len_bytes = bytes.putShort((short)msgLen).array();
            for (int i = 0; i < 2; i++) {
                lengthBytes[i + 1] = len_bytes[i];
            }
        } else if (msgLen > 0xffff){
            // next 8 bytes are the length
            lengthBytes = new byte[9];
            lengthBytes[0] = 127;
            ByteBuffer bytes = ByteBuffer.allocate(8);
            byte[] len_bytes = bytes.putLong(msgLen).array();
            for (int i = 0; i < 8; i++) {
                lengthBytes[i + 1] = len_bytes[i];
            }
        } else if (msgLen <= 125){
            lengthBytes = new byte[1];
            lengthBytes[0] = (byte)(msgLen);
        }

        byte[] fin = new byte[1 + lengthBytes.length + msgLen];
        fin[0] = first;
        for (int i = 0; i < lengthBytes.length; i++) {
            fin[i + 1] = lengthBytes[i];
        }
        int length_offset = lengthBytes.length + 1;
        System.arraycopy(msg, 0, fin, length_offset, msgLen);

        ctx.write(fin);
        ctx.send();
    }

    public static void sendCompressed(Channel ctx, byte[] msg, MessageType type){
        int msgLen = msg.length;
        byte first;
        if(type == MessageType.TEXT)
            first = (byte)0xC1; // turn on compression reserved bit
        else
            first = (byte)0xC2;
        byte[] lengthBytes = null;

        if(msgLen > 125 && msgLen <= 0xffff) {
            // next 2 bytes are the length
            lengthBytes = new byte[3]; // 2 + 1
            lengthBytes[0] = 126; // this indicates to the client that the next 2 bytes are the length
            ByteBuffer bytes = ByteBuffer.allocate(2);
            byte[] len_bytes = bytes.putShort((short)msgLen).array();
            for (int i = 0; i < 2; i++) {
                lengthBytes[i + 1] = len_bytes[i];
            }
        } else if (msgLen > 0xffff){
            // next 8 bytes are the length
            lengthBytes = new byte[9];
            lengthBytes[0] = 127;
            ByteBuffer bytes = ByteBuffer.allocate(8);
            byte[] len_bytes = bytes.putLong(msgLen).array();
            for (int i = 0; i < 8; i++) {
                lengthBytes[i + 1] = len_bytes[i];
            }
        } else if (msgLen <= 125){
            lengthBytes = new byte[1];
            lengthBytes[0] = (byte)(msgLen);
        }

        byte[] fin = new byte[1 + lengthBytes.length + msgLen];
        fin[0] = first;
        for (int i = 0; i < lengthBytes.length; i++) {
            fin[i + 1] = lengthBytes[i];
        }
        int length_offset = lengthBytes.length + 1;
        System.arraycopy(msg, 0, fin, length_offset, msgLen);
        ctx.write(fin);
        ctx.send();
    }

    public static void sendPong(Channel ctx, byte[] body){
        byte[] bytes = new byte[2 + body.length];
        bytes[0] = (byte)0x8a;
        bytes[1] = (byte)body.length;
        for (int i = 0; i < body.length; i++) {
            bytes[i + 2] = body[i];
        }
        ctx.write(bytes);
        Log.debug("Pong sent");
        ctx.send();
    }

    final protected static char[] hexArray = "0123456789abcdef".toCharArray();

    public static String bytesToHex(byte[] bytes) {
        char[] hexChars = new char[bytes.length * 2];
        for ( int j = 0; j < bytes.length; j++ ) {
            int v = bytes[j] & 0xFF;
            hexChars[j * 2] = hexArray[v >>> 4];
            hexChars[j * 2 + 1] = hexArray[v & 0x0F];
        }
        return new String(hexChars);
    }

    public static void sendPing(Channel ctx){
        byte[] bytes = new byte[2];
        bytes[0] = (byte)0x89;
        bytes[1] = 0;
        ctx.write(bytes);
        ctx.send();
    }

    public static void sendClose(Channel ctx){
        sendCloseWithCode(ctx, WebSocketStatusCode.NORMAL_CLOSURE, "");
    }

    public static void sendError(Channel ctx, WebSocketStatusCode statusCode){
        sendCloseWithCode(ctx, statusCode.getStatusCode(), statusCode.getDescription());
    }

    private static void sendCloseWithCode(Channel ctx, short code, String description){
        byte[] bytes = new byte[4 + description.length()];
        bytes[0] = (byte)0x88;
        bytes[1] = (byte)(2 + description.length()); // length
        byte[] status = ByteBuffer.allocate(2).putShort(code).order(ByteOrder.BIG_ENDIAN).array();
        for (int i = 0; i < status.length; i++) {
            bytes[i + 2] = status[i];
        }
        System.arraycopy(description.getBytes(), 0, bytes, 4, description.length());
        ctx.write(bytes);
        ctx.send();
        ctx.close();
        ctx.input().clear();
    }
}
