package com.example.sheepmusic.repository;

import com.example.sheepmusic.entity.CommentLike;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 评论点赞Repository
 */
@Repository
public interface CommentLikeRepository extends JpaRepository<CommentLike, Long> {
    
    /**
     * 查询用户是否点赞某条评论
     */
    Optional<CommentLike> findByCommentIdAndUserId(Long commentId, Long userId);
    
    /**
     * 判断用户是否点赞某条评论
     */
    boolean existsByCommentIdAndUserId(Long commentId, Long userId);
    
    /**
     * 删除点赞记录
     */
    void deleteByCommentIdAndUserId(Long commentId, Long userId);
    
    /**
     * 查询评论的所有点赞记录
     */
    List<CommentLike> findByCommentId(Long commentId);
    
    /**
     * 统计评论点赞数
     */
    long countByCommentId(Long commentId);
}

