package com.example.sheepmusic.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;

/**
 * 动态请求DTO
 */
@Data
public class MomentRequest {
    
    /**
     * 动态类型：text, song, playlist, image
     */
    @NotBlank(message = "动态类型不能为空")
    private String type;
    
    /**
     * 动态内容
     */
    private String content;
    
    /**
     * 关联的歌曲/歌单ID
     */
    private Long relatedId;
    
    /**
     * 图片URLs（JSON数组）
     */
    private String images;
    
    /**
     * 可见范围：public, friends, private
     */
    private String visibility = "public";
}

