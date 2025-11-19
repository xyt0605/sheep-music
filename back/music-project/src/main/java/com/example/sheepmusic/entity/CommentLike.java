package com.example.sheepmusic.entity;

import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;

import javax.persistence.*;
import java.time.LocalDateTime;

/**
 * 评论点赞实体类
 */
@Data
@Entity
@Table(name = "tb_comment_like",
       uniqueConstraints = @UniqueConstraint(columnNames = {"commentId", "userId"}))
public class CommentLike {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    /**
     * 评论ID
     */
    @Column(nullable = false)
    private Long commentId;
    
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

