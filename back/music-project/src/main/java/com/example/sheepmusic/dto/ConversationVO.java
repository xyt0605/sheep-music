package com.example.sheepmusic.dto;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 会话列表VO（用于前端展示）
 */
@Data
public class ConversationVO {
    
    /**
     * 会话ID
     */
    private Long id;
    
    /**
     * 好友ID
     */
    private Long friendId;
    
    /**
     * 好友昵称
     */
    private String friendName;
    
    /**
     * 好友头像
     */
    private String friendAvatar;
    
    /**
     * 最后一条消息内容
     */
    private String lastMessage;
    
    /**
     * 最后一条消息时间
     */
    private LocalDateTime lastMessageTime;
    
    /**
     * 未读消息数
     */
    private Integer unreadCount;
}

