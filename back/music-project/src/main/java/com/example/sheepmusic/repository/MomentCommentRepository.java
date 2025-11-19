package com.example.sheepmusic.repository;

import com.example.sheepmusic.entity.MomentComment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 动态评论Repository
 */
@Repository
public interface MomentCommentRepository extends JpaRepository<MomentComment, Long> {
    
    /**
     * 查询动态的评论（分页）
     */
    Page<MomentComment> findByMomentIdAndParentIdIsNullOrderByCreateTimeDesc(
        Long momentId, Pageable pageable);
    
    /**
     * 查询动态的所有评论
     */
    List<MomentComment> findByMomentIdOrderByCreateTimeDesc(Long momentId);
    
    /**
     * 查询回复
     */
    List<MomentComment> findByParentIdOrderByCreateTimeAsc(Long parentId);
    
    /**
     * 统计动态评论数
     */
    long countByMomentId(Long momentId);
    
    /**
     * 统计用户评论数
     */
    long countByUserId(Long userId);
}



