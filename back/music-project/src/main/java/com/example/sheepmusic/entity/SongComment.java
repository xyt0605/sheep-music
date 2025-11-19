package com.example.sheepmusic.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import javax.persistence.*;
import java.time.LocalDateTime;

/**
 * 歌曲评论实体类
 */
@Data
@Entity
@Table(name = "tb_song_comment")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class SongComment {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    /**
     * 歌曲ID
     */
    @Column(nullable = false)
    private Long songId;
    
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
     * 评分（1-5星，可选）
     */
    @Column
    private Integer rating;
    
    /**
     * 父评论ID（用于回复功能，null表示顶级评论）
     */
    @Column
    private Long parentId;
    
    /**
     * 点赞数
     */
    @Column(nullable = false)
    private Integer likeCount = 0;
    
    /**
     * 是否置顶
     */
    @Column(nullable = false)
    private Boolean pinned = false;
    
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
     * 关联用户信息（用于展示，懒加载）
     * 注意：使用 @JsonIgnore 避免序列化懒加载代理对象，使用冗余字段 username 和 userAvatar
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "userId", insertable = false, updatable = false)
    @com.fasterxml.jackson.annotation.JsonIgnore
    private User user;
    
    /**
     * 冗余字段：用户昵称（避免每次都查询用户表）
     */
    @Column(length = 50)
    private String username;
    
    /**
     * 冗余字段：用户头像
     */
    @Column(length = 500)
    private String userAvatar;
}

