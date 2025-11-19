package com.example.sheepmusic.repository;

import com.example.sheepmusic.entity.PlaylistShare;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 歌单分享Repository
 */
@Repository
public interface PlaylistShareRepository extends JpaRepository<PlaylistShare, Long> {
    
    /**
     * 查询分享广场（所有公开分享，按创建时间倒序）
     */
    Page<PlaylistShare> findAllByOrderByCreateTimeDesc(Pageable pageable);
    
    /**
     * 查询热门分享（按浏览次数倒序）
     */
    Page<PlaylistShare> findAllByOrderByViewCountDesc(Pageable pageable);
    
    /**
     * 查询用户的分享
     */
    List<PlaylistShare> findByUserIdOrderByCreateTimeDesc(Long userId);
    
    /**
     * 查询好友的分享
     */
    @Query("SELECT s FROM PlaylistShare s WHERE s.userId IN :friendIds " +
           "ORDER BY s.createTime DESC")
    List<PlaylistShare> findFriendsShares(@Param("friendIds") List<Long> friendIds);
    
    /**
     * 统计歌单被分享次数
     */
    long countByPlaylistId(Long playlistId);
    
    /**
     * 统计用户分享次数
     */
    long countByUserId(Long userId);
}

