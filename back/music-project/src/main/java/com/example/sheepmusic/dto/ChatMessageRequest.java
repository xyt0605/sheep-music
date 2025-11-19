package com.example.sheepmusic.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

/**
 * 聊天消息请求DTO
 */
@Data
public class ChatMessageRequest {
    
    /**
     * 接收者ID
     */
    @NotNull(message = "接收者ID不能为空")
    private Long receiverId;
    
    /**
     * 消息类型：text, image, audio, playlist, song
     */
    @NotBlank(message = "消息类型不能为空")
    private String type;
    
    /**
     * 消息内容
     */
    @NotBlank(message = "消息内容不能为空")
    private String content;
    
    /**
     * 扩展数据（JSON格式，用于分享歌曲/歌单）
     */
    private String extra;
}

