package com.example.sheepmusic.entity;

import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import javax.persistence.*;
import java.time.LocalDateTime;

/**
 * 会话实体类（用于优化聊天列表查询）
 */
@Data
@Entity
@Table(name = "tb_conversation",
       uniqueConstraints = @UniqueConstraint(columnNames = {"userId", "friendId"}))
public class Conversation {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    /**
     * 用户ID
     */
    @Column(nullable = false)
    private Long userId;
    
    /**
     * 好友ID
     */
    @Column(nullable = false)
    private Long friendId;
    
    /**
     * 最后一条消息ID
     */
    private Long lastMessageId;
    
    /**
     * 最后一条消息内容（冗余）
     */
    @Column(length = 200)
    private String lastMessageContent;
    
    /**
     * 最后一条消息时间（冗余）
     */
    private LocalDateTime lastMessageTime;
    
    /**
     * 未读消息数
     */
    @Column(nullable = false)
    private Integer unreadCount = 0;
    
    /**
     * 创建时间
     */
    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createTime;
    
    /**
     * 更新时间
     */
    @UpdateTimestamp
    private LocalDateTime updateTime;
    
    /**
     * 冗余字段：好友昵称
     */
    @Column(length = 50)
    private String friendName;
    
    /**
     * 冗余字段：好友头像
     */
    @Column(length = 500)
    private String friendAvatar;
}

