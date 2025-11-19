package com.example.sheepmusic.entity;

import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;

import javax.persistence.*;
import java.time.LocalDateTime;

/**
 * 通知实体类
 */
@Data
@Entity
@Table(name = "tb_notification")
public class Notification {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    /**
     * 接收者ID
     */
    @Column(nullable = false)
    private Long userId;
    
    /**
     * 通知类型：comment-评论, like-点赞, friend_request-好友请求, 
     *          friend_accept-好友接受, moment_like-动态点赞, moment_comment-动态评论
     */
    @Column(nullable = false, length = 30)
    private String type;
    
    /**
     * 发送者ID（谁触发的通知）
     */
    @Column
    private Long senderId;
    
    /**
     * 通知标题
     */
    @Column(length = 100)
    private String title;
    
    /**
     * 通知内容
     */
    @Column(length = 500)
    private String content;
    
    /**
     * 关联对象ID（评论ID、歌曲ID、动态ID等）
     */
    @Column
    private Long relatedId;
    
    /**
     * 关联对象类型（comment、song、moment等）
     */
    @Column(length = 30)
    private String relatedType;
    
    /**
     * 跳转链接
     */
    @Column(length = 200)
    private String link;
    
    /**
     * 是否已读
     */
    @Column(nullable = false)
    private Boolean isRead = false;
    
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

