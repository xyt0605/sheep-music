package com.example.sheepmusic.repository;

import com.example.sheepmusic.entity.Friendship;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 好友关系Repository
 */
@Repository
public interface FriendshipRepository extends JpaRepository<Friendship, Long> {
    
    /**
     * 查询用户的所有好友（已接受状态）
     */
    List<Friendship> findByUserIdAndStatus(Long userId, String status);
    
    /**
     * 查询用户收到的好友请求（待确认状态）
     */
    List<Friendship> findByFriendIdAndStatus(Long friendId, String status);
    
    /**
     * 查询两个用户之间的好友关系
     */
    Optional<Friendship> findByUserIdAndFriendId(Long userId, Long friendId);
    
    /**
     * 判断是否为好友关系（双向）
     */
    @Query("SELECT CASE WHEN COUNT(f) > 0 THEN true ELSE false END FROM Friendship f " +
           "WHERE ((f.userId = :userId AND f.friendId = :friendId) " +
           "OR (f.userId = :friendId AND f.friendId = :userId)) " +
           "AND f.status = 'accepted'")
    boolean areFriends(@Param("userId") Long userId, @Param("friendId") Long friendId);
    
    /**
     * 查询用户的好友数量
     */
    long countByUserIdAndStatus(Long userId, String status);
    
    /**
     * 删除好友关系（双向）
     */
    void deleteByUserIdAndFriendId(Long userId, Long friendId);
}

