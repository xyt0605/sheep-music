package com.example.sheepmusic.service;

import com.example.sheepmusic.entity.Friendship;
import com.example.sheepmusic.entity.User;
import com.example.sheepmusic.repository.FriendshipRepository;
import com.example.sheepmusic.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * 好友Service
 */
@Service
public class FriendshipService {
    
    @Autowired
    private FriendshipRepository friendshipRepository;
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private NotificationService notificationService;
    
    @Autowired
    private ConversationService conversationService;
    
    /**
     * 发送好友请求
     */
    @Transactional
    public Friendship sendFriendRequest(Long userId, Long friendId, String remark) {
        if (userId.equals(friendId)) {
            throw new RuntimeException("不能添加自己为好友");
        }
        
        // 检查目标用户是否存在
        User friend = userRepository.findById(friendId)
            .orElseThrow(() -> new RuntimeException("用户不存在"));
        
        // 检查是否已经是好友
        if (friendshipRepository.areFriends(userId, friendId)) {
            throw new RuntimeException("已经是好友关系");
        }
        
        // 获取发起者信息
        User requester = userRepository.findById(userId)
            .orElseThrow(() -> new RuntimeException("用户不存在"));
        
        // 检查是否已发送请求
        Optional<Friendship> existing = friendshipRepository.findByUserIdAndFriendId(userId, friendId);
        Friendship friendship;
        
        if (existing.isPresent()) {
            friendship = existing.get();
            if ("pending".equals(friendship.getStatus())) {
                throw new RuntimeException("已发送好友请求，请等待对方确认");
            }
            // 如果之前被拒绝，重新设置为 pending 状态
            if ("rejected".equals(friendship.getStatus())) {
                friendship.setStatus("pending");
                friendship.setRemark(remark);
                // 更新冗余信息
                friendship.setFriendName(friend.getNickname());
                friendship.setFriendAvatar(friend.getAvatar());
                friendship.setUserName(requester.getNickname());
                friendship.setUserAvatar(requester.getAvatar());
            }
        } else {
            // 创建新的好友请求
            friendship = new Friendship();
            friendship.setUserId(userId);
            friendship.setFriendId(friendId);
            friendship.setStatus("pending");
            friendship.setRemark(remark);
            
            // 冗余好友信息（接收者）
            friendship.setFriendName(friend.getNickname());
            friendship.setFriendAvatar(friend.getAvatar());
            
            // 冗余发起者信息（用于好友请求列表显示）
            friendship.setUserName(requester.getNickname());
            friendship.setUserAvatar(requester.getAvatar());
        }
        
        Friendship saved = friendshipRepository.save(friendship);
        
        // 创建通知
        notificationService.createFriendRequestNotification(friendId, userId);
        
        return saved;
    }
    
    /**
     * 接受好友请求
     */
    @Transactional
    public void acceptFriendRequest(Long friendshipId, Long currentUserId) {
        Friendship friendship = friendshipRepository.findById(friendshipId)
            .orElseThrow(() -> new RuntimeException("好友请求不存在"));
        
        // 只有被请求的人可以接受
        if (!friendship.getFriendId().equals(currentUserId)) {
            throw new RuntimeException("无权操作该好友请求");
        }
        
        if (!"pending".equals(friendship.getStatus())) {
            throw new RuntimeException("该好友请求已处理");
        }
        
        // 更新状态
        friendship.setStatus("accepted");
        friendshipRepository.save(friendship);
        
        // 创建双向好友关系（为对方也创建一条记录）
        User currentUser = userRepository.findById(currentUserId)
            .orElseThrow(() -> new RuntimeException("用户不存在"));
        
        // 获取发起请求的用户信息
        User requester = userRepository.findById(friendship.getUserId())
            .orElseThrow(() -> new RuntimeException("用户不存在"));
        
        Friendship reverse = new Friendship();
        reverse.setUserId(currentUserId);
        reverse.setFriendId(friendship.getUserId());
        reverse.setStatus("accepted");
        reverse.setFriendName(requester.getNickname());
        reverse.setFriendAvatar(requester.getAvatar());
        
        friendshipRepository.save(reverse);
        
        // 创建会话记录
        conversationService.createOrUpdateConversation(currentUserId, friendship.getUserId());
        conversationService.createOrUpdateConversation(friendship.getUserId(), currentUserId);
        
        // 创建通知
        notificationService.createFriendAcceptNotification(
            friendship.getUserId(), currentUserId);
    }
    
    /**
     * 拒绝好友请求
     */
    @Transactional
    public void rejectFriendRequest(Long friendshipId, Long currentUserId) {
        Friendship friendship = friendshipRepository.findById(friendshipId)
            .orElseThrow(() -> new RuntimeException("好友请求不存在"));
        
        if (!friendship.getFriendId().equals(currentUserId)) {
            throw new RuntimeException("无权操作该好友请求");
        }
        
        if (!"pending".equals(friendship.getStatus())) {
            throw new RuntimeException("该好友请求已处理");
        }
        
        friendship.setStatus("rejected");
        friendshipRepository.save(friendship);
    }
    
    /**
     * 删除好友
     */
    @Transactional
    public void deleteFriend(Long friendshipId, Long currentUserId) {
        Friendship friendship = friendshipRepository.findById(friendshipId)
            .orElseThrow(() -> new RuntimeException("好友关系不存在"));
        
        if (!friendship.getUserId().equals(currentUserId)) {
            throw new RuntimeException("无权操作该好友关系");
        }
        
        // 删除双向好友关系
        friendshipRepository.delete(friendship);
        friendshipRepository.deleteByUserIdAndFriendId(
            friendship.getFriendId(), currentUserId);
    }
    
    /**
     * 获取好友列表
     */
    public List<Friendship> getFriendList(Long userId) {
        List<Friendship> friendships = friendshipRepository.findByUserIdAndStatus(userId, "accepted");
        // 强制从 User 表获取最新的好友信息（确保昵称和头像是最新的）
        for (Friendship friendship : friendships) {
            User friend = userRepository.findById(friendship.getFriendId()).orElse(null);
            if (friend != null) {
                // 总是更新为最新的用户信息
                friendship.setFriendName(friend.getNickname());
                friendship.setFriendAvatar(friend.getAvatar());
                // 注意：不保存到数据库，只在内存中更新，避免频繁写库
                // 这样可以确保每次调用都返回最新数据，同时不影响性能
            }
        }
        return friendships;
    }
    
    /**
     * 获取好友请求列表
     */
    public List<Friendship> getFriendRequests(Long userId) {
        List<Friendship> friendships = friendshipRepository.findByFriendIdAndStatus(userId, "pending");
        // 确保冗余字段已填充（填充发起者的信息）
        for (Friendship friendship : friendships) {
            // 对于好友请求，friendId是接收者（当前用户），userId是发起者
            if (friendship.getUserName() == null || friendship.getUserAvatar() == null) {
                User requester = userRepository.findById(friendship.getUserId()).orElse(null);
                if (requester != null) {
                    friendship.setUserName(requester.getNickname());
                    friendship.setUserAvatar(requester.getAvatar());
                    friendshipRepository.save(friendship);
                }
            }
        }
        return friendships;
    }
    
    /**
     * 搜索用户（排除自己和已是好友的用户）
     */
    public List<User> searchUsers(String keyword, Long currentUserId) {
        List<User> users = userRepository.findByUsernameContainingOrNicknameContaining(
            keyword, keyword);
        
        // 过滤掉自己
        users.removeIf(user -> user.getId().equals(currentUserId));
        
        return users;
    }
    
    /**
     * 设置备注名
     */
    @Transactional
    public void setRemark(Long friendshipId, Long currentUserId, String remark) {
        Friendship friendship = friendshipRepository.findById(friendshipId)
            .orElseThrow(() -> new RuntimeException("好友关系不存在"));
        
        if (!friendship.getUserId().equals(currentUserId)) {
            throw new RuntimeException("无权操作该好友关系");
        }
        
        friendship.setRemark(remark);
        friendshipRepository.save(friendship);
    }
    
    /**
     * 检查是否为好友
     */
    public boolean areFriends(Long userId, Long friendId) {
        return friendshipRepository.areFriends(userId, friendId);
    }
}

