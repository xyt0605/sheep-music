package com.example.sheepmusic.repository;

import com.example.sheepmusic.entity.ChatMessage;
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
 * 聊天消息Repository
 */
@Repository
public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {
    
    /**
     * 查询两个用户之间的聊天记录（分页）
     */
    @Query("SELECT m FROM ChatMessage m WHERE " +
           "(m.senderId = :userId AND m.receiverId = :friendId) " +
           "OR (m.senderId = :friendId AND m.receiverId = :userId) " +
           "ORDER BY m.createTime DESC")
    Page<ChatMessage> findChatHistory(@Param("userId") Long userId, 
                                      @Param("friendId") Long friendId, 
                                      Pageable pageable);
    
    /**
     * 查询用户的未读消息
     */
    List<ChatMessage> findByReceiverIdAndIsReadFalseOrderByCreateTimeDesc(Long receiverId);
    
    /**
     * 统计用户的未读消息数
     */
    long countByReceiverIdAndIsReadFalse(Long receiverId);
    
    /**
     * 统计来自某个好友的未读消息数
     */
    long countBySenderIdAndReceiverIdAndIsReadFalse(Long senderId, Long receiverId);
    
    /**
     * 批量标记消息为已读
     */
    @Modifying
    @Query("UPDATE ChatMessage m SET m.isRead = true, m.readTime = :readTime " +
           "WHERE m.id IN :messageIds")
    void markAsRead(@Param("messageIds") List<Long> messageIds, 
                    @Param("readTime") LocalDateTime readTime);
    
    /**
     * 标记与某人的所有未读消息为已读
     */
    @Modifying
    @Query("UPDATE ChatMessage m SET m.isRead = true, m.readTime = :readTime " +
           "WHERE m.senderId = :senderId AND m.receiverId = :receiverId AND m.isRead = false")
    void markAllAsReadBetween(@Param("senderId") Long senderId,
                              @Param("receiverId") Long receiverId,
                              @Param("readTime") LocalDateTime readTime);
    
    /**
     * 查询最后一条消息
     */
    @Query("SELECT m FROM ChatMessage m WHERE " +
           "(m.senderId = :userId AND m.receiverId = :friendId) " +
           "OR (m.senderId = :friendId AND m.receiverId = :userId) " +
           "ORDER BY m.createTime DESC")
    List<ChatMessage> findLastMessage(@Param("userId") Long userId, 
                                      @Param("friendId") Long friendId,
                                      Pageable pageable);
}

