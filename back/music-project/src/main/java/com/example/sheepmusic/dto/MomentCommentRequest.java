package com.example.sheepmusic.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

/**
 * 动态评论请求DTO
 */
@Data
public class MomentCommentRequest {
    
    /**
     * 动态ID
     */
    @NotNull(message = "动态ID不能为空")
    private Long momentId;
    
    /**
     * 评论内容
     */
    @NotBlank(message = "评论内容不能为空")
    private String content;
    
    /**
     * 父评论ID（回复时使用）
     */
    private Long parentId;
}

