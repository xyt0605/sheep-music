package com.example.sheepmusic.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;

/**
 * 更新用户信息请求
 */
@Data
@Schema(description = "更新用户信息请求")
public class UpdateUserRequest {
    
    @Schema(description = "昵称")
    @Size(min = 1, max = 50, message = "昵称长度为1-50个字符")
    private String nickname;
    
    @Schema(description = "邮箱")
    @Email(message = "邮箱格式不正确")
    private String email;
    
    @Schema(description = "头像URL")
    private String avatar;
}

