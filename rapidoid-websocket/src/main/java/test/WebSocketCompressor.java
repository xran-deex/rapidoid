package test;

import org.rapidoid.log.Log;
import org.rapidoid.websocket.IWebSocketExtension;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.zip.DataFormatException;
import java.util.zip.Deflater;
import java.util.zip.Inflater;

/**
 * Created by randy on 8/21/16.
 */
public class WebSocketCompressor implements IWebSocketExtension {

    Deflater deflater;
    Inflater inflater;
    static final int BUFFER_SIZE = 1024;
    byte[] buffer;
    int capacity;
    List<String> extensions;
    boolean no_context_takeover;

    public WebSocketCompressor() {
        deflater = new Deflater(Deflater.DEFAULT_COMPRESSION, true);
        inflater = new Inflater(true);
        capacity = BUFFER_SIZE;
        buffer = new byte[BUFFER_SIZE];
        extensions = new ArrayList<>();
    }

    // server_
    public byte[] compress(byte[] input){
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

        deflater.reset();
        deflater.setInput(input);
        int offset = 0;
        int count = deflater.deflate(buffer, offset, capacity - offset, Deflater.SYNC_FLUSH);
        offset += count;
        while(count > 0){
            if (offset == capacity) {
                // double the buffer capacity
                capacity *= 2;
                buffer = Arrays.copyOf(buffer, capacity);
            }
            count = deflater.deflate(buffer, offset, capacity - offset);
            offset += count;
        }
        outputStream.write(buffer, 0, offset);
        outputStream.write(new byte[]{0}, 0, 1);

        Log.debug("Uncompressed: " + input.length + ", Compressed: " + outputStream.size());
        return outputStream.toByteArray();
    }

    // client_
    public byte[] decompress(byte[] msg){
        if(msg.length == 0){
            return new byte[]{};
        }
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        ByteArrayOutputStream inputStream = new ByteArrayOutputStream(msg.length + 4);
        inputStream.write(msg, 0, msg.length);
        inputStream.write(new byte[] {0, 0, -1, -1 }, 0, 4);
        msg = inputStream.toByteArray();
        int offset = 0;

        if (no_context_takeover) {
            inflater.reset();
        }
        inflater.setInput(msg, 0, msg.length);
        try {
            int count = inflater.inflate(buffer, offset, capacity - offset);
            offset += count;
            while(count > 0){
                if (offset == capacity) {
                    // double the buffer capacity
                    capacity *= 2;
                    buffer = Arrays.copyOf(buffer, capacity);
                }
                count = inflater.inflate(buffer, offset, capacity - offset);
                offset += count;
            }
        } catch (DataFormatException e) {
            e.printStackTrace();
        }
        outputStream.write(buffer, 0, offset);
        Log.debug("Compressed: " + msg.length + ", Uncompressed: " + outputStream.size());
        return outputStream.toByteArray();
    }

    @Override
    public byte[] incoming(byte[] bytes) {
        return decompress(bytes);
    }

    @Override
    public byte[] outgoing(byte[] bytes) {
        return compress(bytes);
    }

    @Override
    public String addExtensionParameter(String param) {
        extensions.add(param);
        switch(param){
            case "client_context_no_takeover":
                no_context_takeover = true;
                return "client_context_no_takeover";
            case "client_max_window_bits":
                return "client_max_window_bits=15";
            case "permessage-deflate":
                return "permessage-deflate";
            default:
                return null;
        }
    }

    @Override
    public IWebSocketExtension getExtension() {
        return new WebSocketCompressor();
    }
}
