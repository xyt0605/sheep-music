package com.example.sheepmusic.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 婉婉小屋素材实体（照片/视频墙）
 */
@Data
@Entity
@Table(name = "tb_memory_item")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class MemoryItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 素材类型：photo-照片, video-视频
     */
    @Column(nullable = false, length = 10)
    private String type;

    /**
     * 媒体文件 URL（OSS memory/ 目录）
     */
    @Column(nullable = false, length = 500)
    private String mediaUrl;

    /**
     * 封面 URL（视频封面，可空；空则前端用视频首帧）
     */
    @Column(length = 500)
    private String coverUrl;

    /**
     * 标题（管理端辨识用）
     */
    @Column(length = 100)
    private String title;

    /**
     * 配文（写给她的那句话）
     */
    @Column(length = 500)
    private String caption;

    /**
     * 拍摄/纪念日期（按月分组与排序依据；空则用 createTime 日期）
     */
    @Column
    private LocalDate memoryDate;

    /**
     * 绑定的本地歌曲 ID（songSource=local 时使用；可空）
     */
    @Column
    private Long songId;

    /**
     * 歌曲来源：local-本地曲库（默认）， gequhai-歌曲海试听源
     */
    @Column(length = 16)
    private String songSource = "local";

    /**
     * 歌曲海外部曲目 ID（songSource=gequhai 时用于拼流式播放地址）
     */
    @Column(length = 64)
    private String songExternalId;

    // ---- 歌曲冗余字段（展示用；歌曲删除后判空兜底，隐藏 ♪ 按钮） ----

    @Column(length = 200)
    private String songTitle;

    @Column(length = 200)
    private String songArtist;

    @Column(length = 500)
    private String songCover;

    /**
     * 状态：published-已发布（展示页可见）， draft-下架（仅管理端可见）
     */
    @Column(nullable = false, length = 20)
    private String status = "published";

    /**
     * 点赞（星星）数冗余，加速列表渲染
     */
    @Column(nullable = false)
    private Integer starCount = 0;

    /**
     * 手动排序权重（同一天内越大越靠前；默认 0）
     */
    @Column(nullable = false)
    private Integer sortOrder = 0;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createTime;
}
