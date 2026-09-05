package com.example.sheepmusic.repository;

import com.example.sheepmusic.entity.SongShareLike;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * 歌曲分享点赞记录Repository
 */
@Repository
public interface SongShareLikeRepository extends JpaRepository<SongShareLike, Long> {

    /**
     * 判断用户是否已点赞某条分享
     */
    boolean existsByShareIdAndUserId(Long shareId, Long userId);

    /**
     * 删除用户对某条分享的点赞记录
     */
    void deleteByShareIdAndUserId(Long shareId, Long userId);
}
