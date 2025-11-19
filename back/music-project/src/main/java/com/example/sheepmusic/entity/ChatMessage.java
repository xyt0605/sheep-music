package com.example.sheepmusic.entity;

import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;

import javax.persistence.*;
import java.time.LocalDateTime;

/**
 * 聊天消息实体类
 */
@Data
@Entity
@Table(name = "tb_chat_message", indexes = {
    @Index(name = "idx_sender_receiver", columnList = "senderId,receiverId"),
    @Index(name = "idx_create_time", columnList = "createTime")
})
public class ChatMessage {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    /**
     * 发送者ID
     */
    @Column(nullable = false)
    private Long senderId;
    
    /**
     * 接收者ID
     */
    @Column(nullable = false)
    private Long receiverId;
    
    /**
     * 消息类型：text-文本, image-图片, audio-语音, playlist-歌单分享, song-歌曲分享
     */
    @Column(nullable = false, length = 20)
    private String type = "text";
    
    /**
     * 消息内容
     */
    @Column(nullable = false, length = 2000)
    private String content;
    
    /**
     * 扩展数据（JSON格式，用于存储分享的歌曲/歌单信息）
     */
    @Column(columnDefinition = "TEXT")
    private String extra;
    
    /**
     * 是否已读
     */
    @Column(nullable = false)
    private Boolean isRead = false;
    
    /**
     * 是否撤回
     */
    @Column(nullable = false)
    private Boolean isRecalled = false;
    
    /**
     * 创建时间
     */
    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createTime;
    
    /**
     * 已读时间
     */
    private LocalDateTime readTime;
    
    /**
     * 冗余字段：发送者昵称
     */
    @Column(length = 50)
    private String senderName;
    
    /**
     * 冗余字段：发送者头像
     */
    @Column(length = 500)
    private String senderAvatar;
}

