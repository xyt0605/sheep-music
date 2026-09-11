package com.example.sheepmusic.repository;

import com.example.sheepmusic.entity.MemoryStar;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 婉婉小屋星星数据访问层
 */
@Repository
public interface MemoryStarRepository extends JpaRepository<MemoryStar, Long> {

    Optional<MemoryStar> findByItemIdAndUserId(Long itemId, Long userId);

    List<MemoryStar> findByItemId(Long itemId);

    long countByUserId(Long userId);

    List<MemoryStar> findByUserId(Long userId);

    /**
     * 近 N 天点亮记录（管理端"星星动态"）
     */
    List<MemoryStar> findByCreateTimeAfterOrderByCreateTimeDesc(LocalDateTime since);
}
