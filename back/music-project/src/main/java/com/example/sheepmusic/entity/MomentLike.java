package com.example.sheepmusic.entity;

import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;

import javax.persistence.*;
import java.time.LocalDateTime;

/**
 * 动态点赞实体类
 */
@Data
@Entity
@Table(name = "tb_moment_like",
       uniqueConstraints = @UniqueConstraint(columnNames = {"momentId", "userId"}))
public class MomentLike {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    /**
     * 动态ID
     */
    @Column(nullable = false)
    private Long momentId;
    
    /**
     * 点赞用户ID
     */
    @Column(nullable = false)
    private Long userId;
    
    /**
     * 创建时间
     */
    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createTime;
}

