package com.example.sheepmusic.repository;

import com.example.sheepmusic.entity.MomentLike;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * 动态点赞Repository
 */
@Repository
public interface MomentLikeRepository extends JpaRepository<MomentLike, Long> {
    
    /**
     * 查询用户是否点赞某条动态
     */
    Optional<MomentLike> findByMomentIdAndUserId(Long momentId, Long userId);
    
    /**
     * 判断用户是否点赞某条动态
     */
    boolean existsByMomentIdAndUserId(Long momentId, Long userId);
    
    /**
     * 删除点赞记录
     */
    void deleteByMomentIdAndUserId(Long momentId, Long userId);
    
    /**
     * 统计动态点赞数
     */
    long countByMomentId(Long momentId);
}



