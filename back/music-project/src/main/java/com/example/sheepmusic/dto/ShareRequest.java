package com.example.sheepmusic.dto;

import lombok.Data;

import javax.validation.constraints.NotNull;

/**
 * 分享请求DTO
 */
@Data
public class ShareRequest {
    
    /**
     * 歌单ID（分享歌单时使用）
     */
    private Long playlistId;
    
    /**
     * 歌曲ID（分享歌曲时使用）
     */
    private Long songId;
    
    /**
     * 分享类型：playlist/song
     */
    @NotNull(message = "分享类型不能为空")
    private String type;
    
    /**
     * 分享描述
     */
    private String description;
}

