package com.example.sheepmusic.dto;

import lombok.Data;

/**
 * 外源开放曲库歌曲（曲库供应链 v1，见 docs/specs/曲库供应链v1/）
 * 无本地 songId——写操作（收藏/歌单/评论/分享）不适用，播放走 streamUrl 同源代理
 */
@Data
public class ExternalSongVO {

    /** 外部曲目 ID（来源内的原始 ID，数字字符串） */
    private String sourceTrackId;

    /** 歌曲标题 */
    private String title;

    /** 歌手名 */
    private String artist;

    /** 专辑名 */
    private String album;

    /** 封面图 URL */
    private String cover;

    /** 时长（秒） */
    private Integer duration;

    /** 播放地址（后端同源流式代理，/api 前缀） */
    private String streamUrl;

    /** 下载地址（可能为空：来源不允许下载时） */
    private String downloadUrl;

    /** 授权简称（如 CC BY-NC-ND 3.0） */
    private String licenseName;

    /** 授权全文链接 */
    private String licenseUrl;
}
