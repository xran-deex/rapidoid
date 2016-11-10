package test;

import org.rapidoid.collection.Coll;
import org.rapidoid.data.JSON;
import org.rapidoid.event.Events;
import org.rapidoid.event.Fire;
import org.rapidoid.http.Req;
import org.rapidoid.http.customize.SessionManager;
import redis.clients.jedis.Jedis;

import java.io.Serializable;
import java.util.Map;

/**
 * Created by randy on 10/29/16.
 */
public class RedisSessionManager implements SessionManager {

    private String prefix = "sess:";
    private Jedis jedis;

    public RedisSessionManager() {
        jedis = new Jedis("localhost");
    }

    @Override
    public Map<String, Serializable> loadSession(Req req, String sessionId) throws Exception {
        Fire.event(Events.SESSION_LOAD, "id", sessionId);
        String data = jedis.get(prefix + sessionId);
        if(data == null) return Coll.concurrentMap();
        Fire.event(Events.SESSION_DESERIALIZE, "id", sessionId);
        Map<String, Serializable> sess = JSON.parse(data);
        Fire.event(Events.SESSION_CONCURRENT_ACCESS, "id", sessionId);
        return sess;
    }

    @Override
    public void saveSession(Req req, String sessionId, Map<String, Serializable> session) throws Exception {
        Fire.event(Events.SESSION_SAVE, "id", sessionId);
        Fire.event(Events.SESSION_SERIALIZE, "id", sessionId);
        String sess = JSON.stringify(session);
        jedis.set(prefix+sessionId, sess);
    }
}
