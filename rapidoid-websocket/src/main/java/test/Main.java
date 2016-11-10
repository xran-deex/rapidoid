package test;

import org.rapidoid.log.Log;
import org.rapidoid.log.LogLevel;
import org.rapidoid.setup.On;

import java.io.File;


public class Main {

    public static void main(String[] args) {
        Log.setLogLevel(LogLevel.ERROR);

        // example of using redis for session storage
        On.custom().sessionManager(new RedisSessionManager());
        On.get("/").html((req, resp) -> {
            req.session();
            return resp.file(new File("index.html"));
        });

        On.websocket("/websocket").ws_init((ws -> {
            // uncomment for compression extension
            // ws.addExtension(new WebSocketCompressor());
        })).ws((ws, req, conn) -> {
            req.session().put("test", "test2");
            System.out.println("Woohoo!");
            System.out.println("Count: " + ws.conns().size());

            conn.onMessage(msg -> {
                System.out.println(conn.session().get("test"));
                System.out.println(conn.session().get("test2"));
                if(msg.isText()) {
                    String m = msg.getString();
                    conn.session("test2", m);
                    conn.session("test3", m.toUpperCase());
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