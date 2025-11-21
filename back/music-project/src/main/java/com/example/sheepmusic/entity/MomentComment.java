package com.example.sheepmusic.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;

import javax.persistence.*;
import java.time.LocalDateTime;

/**
 * 动态评论实体类
 */
@Data
@Entity
@Table(name = "tb_moment_comment")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class MomentComment {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    /**
     * 动态ID
     */
    @Column(nullable = false)
    private Long momentId;
    
    /**
     * 评论用户ID
     */
    @Column(nullable = false)
    private Long userId;
    
    /**
     * 评论内容
     */
    @Column(nullable = false, length = 500)
    private String content;
    
    /**
     * 父评论ID（用于回复功能）
     */
    @Column
    private Long parentId;
    
    /**
     * 创建时间
     */
    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createTime;
    
    /**
     * 关联用户（懒加载）
     * 注意：使用 @JsonIgnore 避免序列化懒加载代理对象，使用冗余字段 username 和 userAvatar
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "userId", insertable = false, updatable = false)
    @com.fasterxml.jackson.annotation.JsonIgnore
    private User user;
    
    /**
     * 冗余字段：用户昵称
     */
    @Column(length = 50)
    private String username;
    
    /**
     * 冗余字段：用户头像
     */
    @Column(length = 500)
    private String userAvatar;
}

