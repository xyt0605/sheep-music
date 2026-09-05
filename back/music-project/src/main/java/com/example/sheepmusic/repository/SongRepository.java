package com.example.sheepmusic.repository;

import com.example.sheepmusic.entity.Song;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 歌曲数据访问层
 */
@Repository
public interface SongRepository extends JpaRepository<Song, Long> {
    
    /**
     * 根据歌手ID查找歌曲（多对多关系）
     */
    @Query("SELECT DISTINCT s FROM Song s JOIN s.artists a WHERE a.id = :artistId")
    List<Song> findByArtistId(@Param("artistId") Long artistId);
    
    /**
     * 根据专辑ID查找歌曲
     */
    List<Song> findByAlbumId(Long albumId);
    
    /**
     * 根据状态查找歌曲（分页）
     */
    Page<Song> findByStatus(Integer status, Pageable pageable);
    
    /**
     * 搜索歌曲（根据歌曲名或歌手名）- 支持多歌手
     */
    @Query("SELECT DISTINCT s FROM Song s LEFT JOIN s.artists a WHERE s.title LIKE %:keyword% OR a.name LIKE %:keyword%")
    Page<Song> searchByKeyword(@Param("keyword") String keyword, Pageable pageable);

    @Query("SELECT DISTINCT s FROM Song s LEFT JOIN s.artists a " +
           "WHERE (LOWER(s.title) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
           "OR LOWER(a.name) LIKE LOWER(CONCAT('%', :keyword, '%'))) " +
           "AND s.status = :status")
    Page<Song> searchByKeywordAndStatus(@Param("keyword") String keyword,
                                        @Param("status") Integer status,
                                        Pageable pageable);
    
    /**
     * 获取热门歌曲（按播放次数排序）
     */
    Page<Song> findByStatusOrderByPlayCountDesc(Integer status, Pageable pageable);
    
    /**
     * 获取最新歌曲（按创建时间排序）
     */
    Page<Song> findByStatusOrderByCreateTimeDesc(Integer status, Pageable pageable);

    /**
     * 原子递增播放次数
     */
    @Modifying
    @Query("UPDATE Song s SET s.playCount = s.playCount + 1 WHERE s.id = :songId")
    void incrementPlayCount(@Param("songId") Long songId);

    // ==================== 推荐系统 v2：有界查询 ====================

    /**
     * 按风格召回上架歌曲（内容通道，genres 有界，结果有 LIMIT）
     */
    List<Song> findByStatusAndGenreInOrderByPlayCountDesc(Integer status, List<String> genres, Pageable pageable);

    /**
     * 按语言召回上架歌曲（内容通道，languages 有界，结果有 LIMIT）
     */
    List<Song> findByStatusAndLanguageInOrderByPlayCountDesc(Integer status, List<String> languages, Pageable pageable);

    /**
     * 按歌手批量召回上架歌曲（歌手通道，artistIds 有界，结果有 LIMIT）
     */
    @Query("SELECT DISTINCT s FROM Song s JOIN s.artists a " +
           "WHERE a.id IN :artistIds AND s.status = 1 " +
           "ORDER BY s.playCount DESC")
    List<Song> findByArtistIdsAndStatusOrderByPlayCountDesc(@Param("artistIds") List<Long> artistIds, Pageable pageable);

    /**
     * 指定时间之后创建的上架歌曲（新鲜度通道，结果有 LIMIT）
     */
    List<Song> findByStatusAndCreateTimeAfterOrderByPlayCountDesc(Integer status, LocalDateTime createTime, Pageable pageable);
}

