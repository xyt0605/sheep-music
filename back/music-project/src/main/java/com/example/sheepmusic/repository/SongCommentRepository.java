package com.example.sheepmusic.repository;

import com.example.sheepmusic.entity.SongComment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 歌曲评论Repository
 */
@Repository
public interface SongCommentRepository extends JpaRepository<SongComment, Long> {
    
    /**
     * 根据歌曲ID查询评论（分页）
     */
    Page<SongComment> findBySongIdAndParentIdIsNullOrderByPinnedDescCreateTimeDesc(
        Long songId, Pageable pageable);
    
    /**
     * 根据歌曲ID查询所有评论（包括回复）
     */
    List<SongComment> findBySongIdOrderByCreateTimeDesc(Long songId);
    
    /**
     * 根据父评论ID查询回复
     */
    List<SongComment> findByParentIdOrderByCreateTimeAsc(Long parentId);
    
    /**
     * 根据用户ID查询评论
     */
    Page<SongComment> findByUserIdOrderByCreateTimeDesc(Long userId, Pageable pageable);
    
    /**
     * 统计歌曲评论数
     */
    long countBySongId(Long songId);
    
    /**
     * 统计用户评论数
     */
    long countByUserId(Long userId);
    
    /**
     * 查询热门评论（点赞数高）
     */
    @Query("SELECT c FROM SongComment c WHERE c.songId = :songId AND c.parentId IS NULL " +
           "ORDER BY c.pinned DESC, c.likeCount DESC, c.createTime DESC")
    Page<SongComment> findHotComments(@Param("songId") Long songId, Pageable pageable);
}

