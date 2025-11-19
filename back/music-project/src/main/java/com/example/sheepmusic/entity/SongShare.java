package com.example.sheepmusic.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;

import javax.persistence.*;
import java.time.LocalDateTime;

/**
 * 歌曲分享实体类
 */
@Data
@Entity
@Table(name = "tb_song_share")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class SongShare {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    /**
     * 歌曲ID
     */
    @Column(nullable = false)
    private Long songId;
    
    /**
     * 分享者ID
     */
    @Column(nullable = false)
    private Long userId;
    
    /**
     * 分享描述
     */
    @Column(length = 500)
    private String description;
    
    /**
     * 浏览次数
     */
    @Column(nullable = false)
    private Integer viewCount = 0;
    
    /**
     * 点赞次数
     */
    @Column(nullable = false)
    private Integer likeCount = 0;
    
    /**
     * 创建时间
     */
    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createTime;
    
    /**
     * 关联歌曲（懒加载）
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "songId", insertable = false, updatable = false)
    @com.fasterxml.jackson.annotation.JsonIgnore
    private Song song;
    
    /**
     * 关联用户（懒加载）
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "userId", insertable = false, updatable = false)
    @com.fasterxml.jackson.annotation.JsonIgnore
    private User user;
    
    /**
     * 冗余字段：歌曲名称
     */
    @Column(length = 100)
    private String songName;
    
    /**
     * 冗余字段：歌曲封面
     */
    @Column(length = 500)
    private String songCover;
    
    /**
     * 冗余字段：艺术家名称
     */
    @Column(length = 100)
    private String artistName;
    
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
