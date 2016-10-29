package test;

import org.rapidoid.log.Log;
import org.rapidoid.log.LogLevel;
import org.rapidoid.setup.On;

import java.io.File;


public class Main {

    public static void main(String[] args) {
        Log.setLogLevel(LogLevel.ERROR);

        On.get("/").html((req, resp) -> {
            return resp.file(new File("index.html"));
        });

        On.websocket("/websocket").ws_init((ws -> {
            // uncomment for compression extension
            //ws.addExtension(new WebSocketCompressor());
        })).ws((ws, req, conn) -> {

            System.out.println("Woohoo!");
            System.out.println("Count: " + ws.conns().size());

            conn.onMessage(msg -> {
                if(msg.isText()) {
                    conn.sendString(msg.getString());
                } else if (msg.isBinary()){
                    conn.sendBytes(msg.getBytes());
                }
            });

            conn.onClose(() -> {
                System.out.println("Goodbye!");
            });
        });
    }
}