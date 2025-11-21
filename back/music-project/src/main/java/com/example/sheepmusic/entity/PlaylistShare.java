package com.example.sheepmusic.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;

import javax.persistence.*;
import java.time.LocalDateTime;

/**
 * 歌单分享实体类
 */
@Data
@Entity
@Table(name = "tb_playlist_share")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class PlaylistShare {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    /**
     * 歌单ID
     */
    @Column(nullable = false)
    private Long playlistId;
    
    /**
     * 分享者ID
     */
    @Column(nullable = false)
    private Long userId;
    
    /**
     * 分享描述
     */
    @Column(length = 200)
    private String description;
    
    /**
     * 浏览次数
     */
    @Column(nullable = false)
    private Integer viewCount = 0;
    
    /**
     * 收藏次数
     */
    @Column(nullable = false)
    private Integer collectCount = 0;
    
    /**
     * 创建时间
     */
    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createTime;
    
    /**
     * 关联歌单（懒加载）
     * 注意：使用 @JsonIgnore 避免序列化懒加载代理对象，使用冗余字段 playlistName 和 playlistCover
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "playlistId", insertable = false, updatable = false)
    @com.fasterxml.jackson.annotation.JsonIgnore
    private Playlist playlist;
    
    /**
     * 关联用户（懒加载）
     * 注意：使用 @JsonIgnore 避免序列化懒加载代理对象，使用冗余字段 userName 和 userAvatar
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "userId", insertable = false, updatable = false)
    @com.fasterxml.jackson.annotation.JsonIgnore
    private User user;
    
    /**
     * 冗余字段：歌单名称
     */
    @Column(length = 100)
    private String playlistName;
    
    /**
     * 冗余字段：歌单封面
     */
    @Column(length = 500)
    private String playlistCover;
    
    /**
     * 冗余字段：分享者昵称
     */
    @Column(length = 50)
    private String userName;
    
    /**
     * 冗余字段：分享者头像
     */
    @Column(length = 500)
    private String userAvatar;
}

