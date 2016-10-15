package org.rapidoid.websocket;

import org.rapidoid.buffer.Buf;
import org.rapidoid.buffer.IncompleteReadException;
import org.rapidoid.data.BufRange;
import org.rapidoid.log.Log;

import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.util.Date;

import static org.rapidoid.websocket.MessageType.CLOSE;
import static org.rapidoid.websocket.MessageType.PING;
import static org.rapidoid.websocket.MessageType.PONG;
import static org.rapidoid.websocket.ParseState.*;
import static org.rapidoid.websocket.WebSocketStatusCode.*;

/**
 * Created by randy on 7/20/16.
 */
public class WebSocketFrame {
    private static short[] INVALID_CLOSE_CODES = { 0, 999, 1004, 1005, 1006, 1012, 1013, 1014, 1015, 1016, 1100, 2000, 2999 };
    private MessageType type;
    private int length;
    private boolean finished;
    private boolean masked;
    private boolean rsv1, rsv2, rsv3;
    private byte[] mask;
    public byte[] bytes;
    byte first, second;
    private boolean hasError;
    private boolean isControlFrame;
    private boolean hasReservedBits;
    private boolean isIncomplete;
    private ParseState parseState = START;
    public Date start, end;
    int messagePosition = 0, bytesNeeded = 0;
    int maskPosition = 0, maskBytesNeeded = 4;
    private byte lengthBytesNeeded;
    private byte[] length_bytes;
    private int startPos, byteCnt = 0;
    WebSocketStatusCode statusCode;

    public WebSocketFrame(){
        mask = new byte[4];
        start = new Date();
        statusCode = new WebSocketStatusCode();
    }

    public WebSocketStatusCode process(Buf buf, boolean compress){
        boolean hasRemaining = false;
        try {
            switch(parseState){
                case START:
                    startPos = buf.position();
                    first = buf.next();
                    byteCnt++;
                    parseFirst(statusCode, first);
                    parseReserved(first);
                    Log.debug("Parsed first byte");
                    parseState = FIN_READ;
                    if(!buf.hasRemaining()){
                        Log.debug("Incomplete frame");
                        isIncomplete = true;
                        statusCode.setStatusCode(NO_ERROR);
                        return statusCode;
                    }
                    if(statusCode.getStatusCode() != NO_ERROR) return statusCode;
                case FIN_READ:
                    second = buf.next();
                    byteCnt++;
                    parseMask(second);
                    Log.debug("Parsed second byte");
                    parseState = MASK_READ;
                case MASK_READ:
                    parseLengthByte(second);
                    if(!buf.hasRemaining()){
                        Log.debug("Incomplete frame");
                        isIncomplete = true;
                        statusCode.setStatusCode(NO_ERROR);
                        return statusCode;
                    }
                    parseState = LEN_READ;
                case LEN_READ:
                    isIncomplete = !parseLength(buf);
                    if(isControlFrame() && length > 125){
                        hasError = true;
                        statusCode.setStatusCode(PROTOCOL_ERROR);
                        statusCode.setDescription("Control frames must be less than 126 bytes");
                        return statusCode;
                    }
                    if(isIncomplete){
                        Log.debug("Incomplete frame");
                        statusCode.setStatusCode(NO_ERROR);
                        return statusCode;
                    }
                    Log.debug("Parsed length");
                    parseState = LEN_PARSE_STARTED;
                case LEN_PARSE_STARTED:
                    isIncomplete = !parseLength(buf);
                    if(isIncomplete){
                        statusCode.setStatusCode(NO_ERROR);
                        return statusCode;
                    }
                    parseState = LEN_PARSE_FIN;
                case LEN_PARSE_FIN:
                    bytes = new byte[length];
                    if ((rsv1 && !compress) || rsv2 || rsv3) {
                        hasError = true;
                        statusCode.setStatusCode(PROTOCOL_ERROR);
                        statusCode.setDescription("Reserved bits used");
                        return statusCode;
                    }
                    parseState = MASK_READ_STARTED;
                case MASK_READ_STARTED:
                    if (!parseMask(buf)) {
                        Log.debug("Incomplete frame");
                        statusCode.setStatusCode(NO_ERROR);
                        return statusCode;
                    }
                    Log.debug("Parsed mask");
                    parseState = MASK_READ_FIN;
                case MASK_READ_FIN:
                    parseState = PAYLOAD_READ_STARTED;
                    try {
                        isIncomplete = !parseMessage(buf);
                    } catch (Exception e){
                        System.out.println(e.getMessage());
                    }
                    if(isIncomplete) {
                        Log.debug("Incomplete frame");
                        statusCode.setStatusCode(NO_ERROR);
                        return statusCode;
                    }
                    Log.debug("Parsed message");
                    parseState = PAYLOAD_READ_FIN;
                case PAYLOAD_READ_STARTED:
                    try {
                        if(parseState == PAYLOAD_READ_STARTED)
                            isIncomplete = !parseMessage(buf);
                    } catch (Exception e){
                        Log.error(e.getMessage());
                    }
                    if(isIncomplete) {
                        statusCode.setStatusCode(NO_ERROR);
                        return statusCode;
                    }
                    parseState = PAYLOAD_READ_FIN;
                case PAYLOAD_READ_FIN:
                    parseState = COMPLETE;
                    if(type == CLOSE && bytes.length > 0){
                        if(bytes.length < 2){
                            statusCode.setStatusCode(PROTOCOL_ERROR);
                            statusCode.setDescription("Invalid close code");
                            return statusCode;
                        }
                        ByteBuffer buffer = ByteBuffer.wrap(bytes);
                        short result = buffer.getShort();
                        if( IsInvalidCloseCode(result) ){
                            hasError = true;
                            statusCode.setStatusCode(PROTOCOL_ERROR);
                            statusCode.setDescription("Invalid close code");
                            return statusCode;
                        }
                    }
                    Log.debug("Buffer position: " + buf.position());
                    Log.debug("Buffer used: " + (buf.position() - startPos));
                    Log.debug("Frame content length: " + (2 + mask.length + length_bytes.length + length));
                    Log.debug("Total byte count: " + byteCnt);
                    end = new Date();
                    Log.debug("Frame has error: " + hasError);
            }

            hasRemaining = buf.hasRemaining();

        } catch( IncompleteReadException e){
            hasRemaining = buf.hasRemaining() && hasRemaining;
            statusCode.setStatusCode(NO_ERROR);
            return statusCode;
        } finally {

        }
        if (hasRemaining) {
            statusCode.setStatusCode(NEED_MORE_BYTES);
            return statusCode;
        }
        statusCode.setStatusCode(NO_ERROR);
        return statusCode;
    }

    private boolean IsInvalidCloseCode(short code){
        for (short invalidCode : INVALID_CLOSE_CODES) {
            if(code == invalidCode) return true;
        }
        return false;
    }

    public static boolean isValidUTF8( ByteBuffer buffer ) {
        // http://stackoverflow.com/questions/14236923/how-to-validate-if-a-utf-8-string-contains-mal-encoded-characters
        CharsetDecoder cs = Charset.forName("UTF-8").newDecoder();

        try {
            cs.decode(buffer);
            return true;
        }
        catch(CharacterCodingException e){
            return false;
        }
    }

    private WebSocketStatusCode parseFirst(WebSocketStatusCode statusCode, byte first){
        Log.debug(""+ first);
        finished = Helper.isFin(first);
        if(!finished) {
            Log.debug("Frame fragment started");
        }
        hasReservedBits = Helper.reserved(first);
        type = Helper.getMessageType(first);
        if(type == MessageType.UNKNOWN){
            hasError = true;
            statusCode.setStatusCode(PROTOCOL_ERROR);
            statusCode.setDescription("Unknown message type");
            return statusCode;
        }
        if(type == MessageType.CONTINUATION){
            Log.debug(""+type);
        }
        if(isControlFrame() && !isFinished()){
            hasError = true;
            statusCode.setStatusCode(PROTOCOL_ERROR);
            statusCode.setDescription("Control frames must not be fragmented");
        }
        Log.debug("Parsed first byte: " + type);
        return statusCode;
    }

    public boolean isContinuation(){
        return type == MessageType.CONTINUATION;
    }

    private void parseMask(byte second){
        masked = Helper.isMasked(second);
    }

    private void parseReserved(byte second){
        rsv1 = Helper.rsv1(second);
        rsv2 = Helper.rsv2(second);
        rsv3 = Helper.rsv3(second);
    }

    private void parseLengthByte(byte second){
        int len = Helper.getMessageLength(second);
        if(len <= 125){
            bytesNeeded = length = len;
            length_bytes = new byte[0];
        } else if (len == 126){
            lengthBytesNeeded = 2;
            length_bytes = new byte[2];
        } else if (len == 127) {
            lengthBytesNeeded = 8;
            length_bytes = new byte[8];
        }
    }

    private boolean parseLength(Buf buf){
        if(lengthBytesNeeded == 0) return true;
        while(lengthBytesNeeded > 0){
            int offset = length_bytes.length - lengthBytesNeeded;
            length_bytes[offset] = buf.next();
            byteCnt++;
            lengthBytesNeeded--;
            if(!buf.hasRemaining()) return false;
        }
        bytesNeeded = length = (int)Helper.parseLengthBytes(length_bytes);
        return true;
    }

    private boolean parseMask(Buf buf){

        // get mask
        if (masked) {
            int pos = buf.position();
            int byteCount = buf.limit() - pos;
            Log.debug("Mask bytes needed: " + maskBytesNeeded);
            if(byteCount >= maskBytesNeeded) {
                byteCount = maskBytesNeeded;
                maskBytesNeeded = 0;
                Log.debug("Got all mask bytes");
            } else {
                maskBytesNeeded -= byteCount;
                if(maskBytesNeeded == 0){
                    Log.debug("Got all mask bytes");
                }
            }
            if(maskBytesNeeded > 0)
                Log.debug("Mask bytes still needed: " + maskBytesNeeded);

            try {
                Log.debug("byte count: " + byteCount);
                BufRange range = new BufRange(pos, byteCount);
                buf.get(range, mask, maskPosition);
                maskPosition += byteCount;
                byteCnt += byteCount;
            } catch (Exception e){
                Log.debug("Error extracting mask");
                Log.error(e.getMessage());
                return false;
            }

            buf.position(buf.position() + byteCount);
        } else if (!masked) {
            statusCode.setStatusCode(PROTOCOL_ERROR);
            statusCode.setDescription("Client messages must be masked");
            hasError = true;
        }
        if(maskBytesNeeded > 0) return false;
        return true;
    }

    private boolean parseMessage(Buf buf){
        if(bytes.length == 0) return true;
        if(bytesNeeded == 0) return true;

        int pos = buf.position();
        int byteCount = buf.limit() - pos;
        if(byteCount > bytesNeeded) byteCount = bytesNeeded;
        else bytesNeeded -= byteCount;
        if(bytesNeeded < 0) byteCount += bytesNeeded;

        try {
            BufRange range = new BufRange(pos, byteCount);
            buf.get(range, bytes, messagePosition);
            if (masked) {
                // unmask
                for (int i = messagePosition; i < messagePosition+byteCount; i++) {
                    bytes[i] = (byte) (bytes[i] ^ mask[i % 4]);
                }
            }
            messagePosition += byteCount;
            byteCnt += byteCount;
        } catch (Exception e){
            System.out.println(e.getMessage());
        }

        buf.position(buf.position() + byteCount);

        if(messagePosition < length) return false;
        Log.debug(Helper.bytesToHex(mask));
        Log.debug(Helper.bytesToHex(bytes));
        return true;
    }

    public byte[] getMessage(){
        return bytes;
    }

    public int getLength(){
        return length;
    }

    public boolean isFinished() {
        return finished;
    }

    public MessageType getType() {
        return type;
    }

    public boolean hasError() {
        return hasError;
    }

    public WebSocketStatusCode getStatusCode() {
        return statusCode;
    }

    public boolean isComplete() {
        return parseState == COMPLETE;
    }

    public boolean isControlFrame() {
        return type == PING || type == PONG || type == CLOSE;
    }

    public String getCloseMessage(){
        if(bytes.length < 2) return "";
        byte[] result = new byte[bytes.length - 2];
        System.arraycopy(bytes, 2, result, 0, bytes.length - 2);
        return new String(result);
    }
}
