package com.example.sheepmusic.agent;

import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 小屋 DJ 会话记忆（agent v1 P2）
 * 服务端内存会话库： sessionId → 最近 10 轮对话 + 上一轮已推荐歌名
 * TTL 30 分钟、总量 200 个会话（synchronized 读写，个人站流量足够）
 */
@Component
public class DjSessionStore {

    public record Turn(String role, String text) {
    }

    public static final class Session {
        private final List<Turn> turns = new ArrayList<>();
        private List<String> lastTitles = new ArrayList<>();
        private Instant lastActive = Instant.now();

        public List<Turn> getTurns() {
            return turns;
        }

        public List<String> getLastTitles() {
            return lastTitles;
        }
    }

    private static final long TTL_MS = 30 * 60 * 1000L;
    private static final int MAX_SESSIONS = 200;
    private static final int MAX_TURNS = 10;

    private final Map<String, Session> sessions = new ConcurrentHashMap<>();

    public synchronized Session get(String sessionId) {
        sweep();
        Session s = sessions.get(sessionId);
        if (s == null) {
            s = new Session();
            sessions.put(sessionId, s);
        }
        s.lastActive = Instant.now();
        return s;
    }

    public synchronized void appendTurn(String sessionId, String role, String text) {
        Session s = get(sessionId);
        s.getTurns().add(new Turn(role, text));
        while (s.getTurns().size() > MAX_TURNS) {
            s.getTurns().remove(0);
        }
    }

    public synchronized void setLastTitles(String sessionId, List<String> titles) {
        get(sessionId).lastTitles = titles == null ? List.of() : titles;
    }

    private void sweep() {
        if (sessions.size() <= MAX_SESSIONS) {
            return;
        }
        Instant cutoff = Instant.now().minusMillis(TTL_MS);
        sessions.entrySet().removeIf(e -> e.getValue().lastActive.isBefore(cutoff));
        while (sessions.size() > MAX_SESSIONS) {
            String oldest = null;
            Instant min = Instant.MAX;
            for (Map.Entry<String, Session> e : sessions.entrySet()) {
                if (e.getValue().lastActive.isBefore(min)) {
                    min = e.getValue().lastActive;
                    oldest = e.getKey();
                }
            }
            if (oldest == null) {
                break;
            }
            sessions.remove(oldest);
        }
    }
}
