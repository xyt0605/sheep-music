package com.example.sheepmusic.agent;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户 AI 连接配置（agent v1 P3，BYOK：每个用户配置自己的模型密钥）
 * apiKey 加密落库（AES/GCM，密钥由 JWT_SECRET 派生），永不回传明文
 */
@Data
@Entity
@Table(name = "tb_user_ai_config")
public class UserAiConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 用户 ID（唯一） */
    @Column(nullable = false, unique = true)
    private Long userId;

    /** 加密后的 API Key（Base64[iv+密文]） */
    @Column(nullable = false, length = 512)
    private String apiKeyEnc;

    /** OpenAI 兼容接口地址（默认智谱） */
    @Column(nullable = false, length = 255)
    private String baseUrl;

    /** 模型名 */
    @Column(nullable = false, length = 64)
    private String model;

    private LocalDateTime updateTime;
}
