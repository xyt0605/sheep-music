package com.example.sheepmusic.repository;

import com.example.sheepmusic.entity.Conversation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 会话Repository
 */
@Repository
public interface ConversationRepository extends JpaRepository<Conversation, Long> {
    
    /**
     * 查询用户的所有会话（按最后消息时间倒序）
     */
    List<Conversation> findByUserIdOrderByLastMessageTimeDesc(Long userId);
    
    /**
     * 查询特定会话
     */
    Optional<Conversation> findByUserIdAndFriendId(Long userId, Long friendId);
    
    /**
     * 统计用户的总未读消息数
     */
    @Query("SELECT SUM(c.unreadCount) FROM Conversation c WHERE c.userId = :userId")
    Long getTotalUnreadCount(@Param("userId") Long userId);
    
    /**
     * 清空会话未读数
     */
    @Modifying
    @Query("UPDATE Conversation c SET c.unreadCount = 0 " +
           "WHERE c.userId = :userId AND c.friendId = :friendId")
    void clearUnreadCount(@Param("userId") Long userId, @Param("friendId") Long friendId);
}

