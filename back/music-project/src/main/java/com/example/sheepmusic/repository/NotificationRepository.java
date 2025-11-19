package com.example.sheepmusic.repository;

import com.example.sheepmusic.entity.Notification;
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
 * 通知Repository
 */
@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {
    
    /**
     * 查询用户的通知（分页）
     */
    Page<Notification> findByUserIdOrderByCreateTimeDesc(Long userId, Pageable pageable);
    
    /**
     * 查询用户的未读通知
     */
    List<Notification> findByUserIdAndIsReadFalseOrderByCreateTimeDesc(Long userId);
    
    /**
     * 统计用户的未读通知数
     */
    long countByUserIdAndIsReadFalse(Long userId);
    
    /**
     * 标记通知为已读
     */
    @Modifying
    @Query("UPDATE Notification n SET n.isRead = true, n.readTime = :readTime " +
           "WHERE n.id = :notificationId")
    void markAsRead(@Param("notificationId") Long notificationId, 
                    @Param("readTime") LocalDateTime readTime);
    
    /**
     * 批量标记通知为已读
     */
    @Modifying
    @Query("UPDATE Notification n SET n.isRead = true, n.readTime = :readTime " +
           "WHERE n.id IN :notificationIds")
    void markBatchAsRead(@Param("notificationIds") List<Long> notificationIds,
                         @Param("readTime") LocalDateTime readTime);
    
    /**
     * 标记所有通知为已读
     */
    @Modifying
    @Query("UPDATE Notification n SET n.isRead = true, n.readTime = :readTime " +
           "WHERE n.userId = :userId AND n.isRead = false")
    void markAllAsRead(@Param("userId") Long userId, 
                       @Param("readTime") LocalDateTime readTime);
    
    /**
     * 删除旧通知（超过指定天数）
     */
    @Modifying
    @Query("DELETE FROM Notification n WHERE n.createTime < :beforeDate")
    void deleteOldNotifications(@Param("beforeDate") LocalDateTime beforeDate);
}



