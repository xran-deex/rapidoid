package test;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.rapidoid.annotation.Controller;
import org.rapidoid.annotation.GET;
import org.rapidoid.data.JSON;
import org.rapidoid.http.Req;
import org.rapidoid.log.Log;
import org.rapidoid.log.LogLevel;
import org.rapidoid.setup.On;
import org.rapidoid.websocket.*;

import java.io.File;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;


public class Main {

    public static void main(String[] args) {
        Log.setLogLevel(LogLevel.DEBUG);

        On.get("/").html((req, resp) -> {
            return resp.file(new File("index.html"));
        });

        On.websocket("/websocket").ws_init((ws -> {

        })).ws((ws, req, conn) -> {

            System.out.println("Woohoo!");
            System.out.println("Count: " + ws.conns().size());
            conn.session("stuff", "Hello"+Math.random());

            conn.onMessage(msg -> {
                if(msg.isText()) {
                    conn.sendString(msg.getString());
                    conn.broadcast(msg.getString(), true);
                    conn.broadcast(conn.session("stuff"), false);
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