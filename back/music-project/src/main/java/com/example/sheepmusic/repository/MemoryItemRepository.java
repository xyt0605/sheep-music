package com.example.sheepmusic.repository;

import com.example.sheepmusic.entity.MemoryItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 婉婉小屋素材数据访问层
 */
@Repository
public interface MemoryItemRepository extends JpaRepository<MemoryItem, Long> {

    /**
     * 按状态查素材，新→旧排序（同日内按 sortOrder 降序微调）
     */
    List<MemoryItem> findByStatusOrderByMemoryDateDescSortOrderDescIdDesc(String status);

    /**
     * 全量素材（管理端，含 draft）
     */
    List<MemoryItem> findByOrderByMemoryDateDescSortOrderDescIdDesc();
}
