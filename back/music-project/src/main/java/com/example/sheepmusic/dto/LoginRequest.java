package com.example.sheepmusic.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;

/**
 * 用户登录请求
 */
@Data
public class LoginRequest {
    
    @NotBlank(message = "用户名不能为空")
    private String username;
    
    @NotBlank(message = "密码不能为空")
    private String password;
}

