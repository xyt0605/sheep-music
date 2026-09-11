package com.example.sheepmusic.entity;

import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * 婉婉小屋星星（她的轻反馈；数据仅管理端"星星动态"可见）
 */
@Data
@Entity
@Table(name = "tb_memory_star", uniqueConstraints = @UniqueConstraint(columnNames = {"itemId", "userId"}))
public class MemoryStar {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long itemId;

    @Column(nullable = false)
    private Long userId;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createTime;
}
