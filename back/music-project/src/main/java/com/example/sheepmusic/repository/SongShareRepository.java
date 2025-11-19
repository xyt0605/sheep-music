package com.example.sheepmusic.repository;

import com.example.sheepmusic.entity.SongShare;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 歌曲分享Repository
 */
@Repository
public interface SongShareRepository extends JpaRepository<SongShare, Long> {
    
    /**
     * 查询分享广场（所有公开分享，按创建时间倒序）
     */
    Page<SongShare> findAllByOrderByCreateTimeDesc(Pageable pageable);
    
    /**
     * 查询热门分享（按浏览次数倒序）
     */
    Page<SongShare> findAllByOrderByViewCountDesc(Pageable pageable);
    
    /**
     * 查询用户的分享
     */
    List<SongShare> findByUserIdOrderByCreateTimeDesc(Long userId);
    
    /**
     * 查询好友的分享
     */
    @Query("SELECT s FROM SongShare s WHERE s.userId IN :friendIds " +
           "ORDER BY s.createTime DESC")
    List<SongShare> findFriendsShares(@Param("friendIds") List<Long> friendIds);
    
    /**
     * 统计歌曲被分享次数
     */
    long countBySongId(Long songId);
    
    /**
     * 统计用户分享次数
     */
    long countByUserId(Long userId);
}
