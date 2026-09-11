package com.example.sheepmusic.agent;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 小屋 DJ 会话记忆（agent v1 P3：持久化到 tb_dj_chat_message，重启不丢）
 * TTL 30 分钟（读取时清理）；每会话保留最近 30 条；lastTitles 由最近一条带 titlesJson 的 DJ 消息派生
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DjSessionStore {

    public record Turn(String role, String text) {
    }

    public record Context(List<Turn> turns, List<String> lastTitles) {
    }

    private static final long TTL_MS = 30 * 60 * 1000L;
    private static final int FETCH_LIMIT = 30;
    private static final int MAX_TURNS = 10;

    private final DjChatMessageRepository repo;

    public Context load(Long userId, String sessionId) {
        LocalDateTime cutoff = LocalDateTime.now().minusMinutes(30);
        try {
            repo.deleteByUserIdAndSessionIdAndCreateTimeBefore(userId, sessionId, cutoff);
        } catch (Exception e) {
            log.debug("会话过期清理失败: {}", e.getMessage());
        }
        List<DjChatMessage> list = new ArrayList<>(repo
                .findByUserIdAndSessionIdOrderByCreateTimeDesc(userId, sessionId, PageRequest.of(0, FETCH_LIMIT))
                .getContent());
        Collections.reverse(list);

        List<Turn> turns = new ArrayList<>();
        List<String> lastTitles = List.of();
        for (DjChatMessage m : list) {
            if (m.getTitlesJson() != null && !m.getTitlesJson().isBlank()) {
                lastTitles = parseTitles(m.getTitlesJson());
            }
            turns.add(new Turn(m.getRole(), m.getContent()));
        }
        while (turns.size() > MAX_TURNS) {
            turns.remove(0);
        }
        return new Context(turns, lastTitles);
    }

    public void appendTurn(Long userId, String sessionId, String role, String text, List<String> titles) {
        DjChatMessage m = new DjChatMessage();
        m.setUserId(userId);
        m.setSessionId(sessionId);
        m.setRole(role);
        m.setContent(text == null ? "" : text.length() > 1900 ? text.substring(0, 1900) : text);
        if (titles != null) {
            try {
                m.setTitlesJson(new com.fasterxml.jackson.databind.ObjectMapper()
                        .writeValueAsString(titles.size() > 12 ? titles.subList(0, 12) : titles));
            } catch (Exception ignored) {
            }
        }
        try {
            repo.save(m);
        } catch (Exception e) {
            log.warn("会话消息写入失败: {}", e.getMessage());
        }
    }

    private List<String> parseTitles(String json) {
        try {
            List<String> titles = new com.fasterxml.jackson.databind.ObjectMapper()
                    .readValue(json, new com.fasterxml.jackson.core.type.TypeReference<List<String>>() {
                    });
            return titles == null ? List.of() : titles;
        } catch (Exception e) {
            return List.of();
        }
    }
}
