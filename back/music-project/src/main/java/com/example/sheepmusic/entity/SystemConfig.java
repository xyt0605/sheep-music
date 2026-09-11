package com.example.sheepmusic.entity;

import lombok.Data;
import org.hibernate.annotations.UpdateTimestamp;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * 系统配置（系统设置 v1）：key-value 行式存储
 * 密钥类值（oss.accessKeyId/oss.accessKeySecret）以 AES/GCM 加密后落库，接口永不回传明文
 */
@Data
@Entity
@Table(name = "tb_system_config", uniqueConstraints = @UniqueConstraint(columnNames = "configKey"))
public class SystemConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 配置键（如 oss.endpoint / oss.accessKeyId） */
    @Column(name = "configKey", nullable = false, unique = true, length = 100)
    private String configKey;

    /** 配置值（TEXT：密钥类为 Base64[iv+密文]） */
    @Column(nullable = false, columnDefinition = "TEXT")
    private String configValue;

    @UpdateTimestamp
    @Column
    private LocalDateTime updateTime;
}
