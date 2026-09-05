package com.example.sheepmusic.entity;

import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;

import javax.persistence.*;
import java.time.LocalDateTime;

/**
 * 歌曲分享点赞记录（用户-分享 唯一，防止重复刷赞）
 */
@Data
@Entity
@Table(name = "tb_song_share_like",
       uniqueConstraints = @UniqueConstraint(columnNames = {"share_id", "user_id"}))
public class SongShareLike {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 分享ID
     */
    @Column(name = "share_id", nullable = false)
    private Long shareId;

    /**
     * 点赞用户ID
     */
    @Column(name = "user_id", nullable = false)
    private Long userId;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createTime;
}
