package com.example.sheepmusic.agent;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 小屋 DJ 会话消息（agent v1 P3：会话记忆持久化，替代内存会话库——重启不丢）
 * titlesJson 仅 DJ 消息携带（该轮候选歌名数组，用于派生"上一轮已推荐"）
 */
@Data
@Entity
@Table(name = "tb_dj_chat_message", indexes = {
        @Index(name = "idx_dj_msg_user_session", columnList = "userId, sessionId, createTime")
})
public class DjChatMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false, length = 64)
    private String sessionId;

    /** user / dj */
    @Column(nullable = false, length = 8)
    private String role;

    @Column(nullable = false, length = 2000)
    private String content;

    /** 该轮候选歌名 JSON 数组（仅 DJ 消息，可空） */
    @Column(length = 1000)
    private String titlesJson;

    @Column(nullable = false)
    private LocalDateTime createTime;
}
