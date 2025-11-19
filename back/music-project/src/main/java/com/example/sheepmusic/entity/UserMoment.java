package com.example.sheepmusic.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;

import javax.persistence.*;
import java.time.LocalDateTime;

/**
 * 用户动态实体类
 */
@Data
@Entity
@Table(name = "tb_user_moment")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class UserMoment {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    /**
     * 用户ID
     */
    @Column(nullable = false)
    private Long userId;
    
    /**
     * 动态类型：text-纯文本, song-分享歌曲, playlist-分享歌单, image-图片
     */
    @Column(nullable = false, length = 20)
    private String type;
    
    /**
     * 动态内容
     */
    @Column(length = 500)
    private String content;
    
    /**
     * 关联的歌曲/歌单ID
     */
    @Column
    private Long relatedId;
    
    /**
     * 图片URLs（JSON数组）
     */
    @Column(columnDefinition = "TEXT")
    private String images;
    
    /**
     * 点赞数
     */
    @Column(nullable = false)
    private Integer likeCount = 0;
    
    /**
     * 评论数
     */
    @Column(nullable = false)
    private Integer commentCount = 0;
    
    /**
     * 可见范围：public-公开, friends-仅好友, private-仅自己
     */
    @Column(nullable = false, length = 20)
    private String visibility = "public";
    
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
    
    /**
     * 冗余字段：关联对象的标题（歌曲标题/歌单名称）
     */
    @Column(length = 200)
    private String relatedTitle;
    
    /**
     * 冗余字段：关联对象的封面
     */
    @Column(length = 500)
    private String relatedCover;
}

