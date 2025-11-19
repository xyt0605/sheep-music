package com.example.sheepmusic.repository;

import com.example.sheepmusic.entity.Artist;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * 歌手数据访问层
 */
@Repository
public interface ArtistRepository extends JpaRepository<Artist, Long> {
    
    /**
     * 根据歌手名称查找
     */
    Optional<Artist> findByName(String name);
    
    /**
     * 按歌曲数量排序获取歌手列表（降序）
     */
    @Query("SELECT a FROM Artist a LEFT JOIN Song s ON a MEMBER OF s.artists " +
           "GROUP BY a.id " +
           "ORDER BY COUNT(s.id) DESC, a.createTime DESC")
    Page<Artist> findAllOrderBySongCountDesc(Pageable pageable);
    
    /**
     * 按歌曲数量排序获取歌手列表（升序）
     */
    @Query("SELECT a FROM Artist a LEFT JOIN Song s ON a MEMBER OF s.artists " +
           "GROUP BY a.id " +
           "ORDER BY COUNT(s.id) ASC, a.createTime DESC")
    Page<Artist> findAllOrderBySongCountAsc(Pageable pageable);
}

