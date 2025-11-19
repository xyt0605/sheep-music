package com.example.sheepmusic.dto;

import lombok.Data;

import javax.validation.constraints.NotNull;

/**
 * 好友请求DTO
 */
@Data
public class FriendRequest {
    
    /**
     * 好友ID
     */
    @NotNull(message = "好友ID不能为空")
    private Long friendId;
    
    /**
     * 备注名（可选）
     */
    private String remark;
}

