package com.example.sheepmusic.dto;

import lombok.Data;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

/**
 * 评论请求DTO
 */
@Data
public class CommentRequest {
    
    /**
     * 歌曲ID
     */
    @NotNull(message = "歌曲ID不能为空")
    private Long songId;
    
    /**
     * 评论内容
     */
    @NotBlank(message = "评论内容不能为空")
    private String content;
    
    /**
     * 评分（可选，1-5星）
     */
    @Min(value = 1, message = "评分最低为1星")
    @Max(value = 5, message = "评分最高为5星")
    private Integer rating;
    
    /**
     * 父评论ID（回复时使用）
     */
    private Long parentId;
}

