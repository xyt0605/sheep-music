package com.example.sheepmusic.repository;

import com.example.sheepmusic.entity.UserMoment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 用户动态Repository
 */
@Repository
public interface UserMomentRepository extends JpaRepository<UserMoment, Long> {
    
    /**
     * 查询用户的动态
     */
    Page<UserMoment> findByUserIdOrderByCreateTimeDesc(Long userId, Pageable pageable);
    
    /**
     * 查询好友动态（仅公开和好友可见）
     */
    @Query("SELECT m FROM UserMoment m WHERE m.userId IN :friendIds " +
           "AND m.visibility IN ('public', 'friends') " +
           "ORDER BY m.createTime DESC")
    Page<UserMoment> findFriendsMoments(@Param("friendIds") List<Long> friendIds, 
                                        Pageable pageable);
    
    /**
     * 查询公开动态
     */
    Page<UserMoment> findByVisibilityOrderByCreateTimeDesc(String visibility, Pageable pageable);
    
    /**
     * 查询热门动态（点赞数高）
     */
    Page<UserMoment> findByVisibilityOrderByLikeCountDescCreateTimeDesc(
        String visibility, Pageable pageable);
    
    /**
     * 统计用户动态数
     */
    long countByUserId(Long userId);
}

