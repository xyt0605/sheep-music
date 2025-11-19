package com.example.sheepmusic.service;

import com.example.sheepmusic.entity.Notification;
import com.example.sheepmusic.entity.User;
import com.example.sheepmusic.repository.NotificationRepository;
import com.example.sheepmusic.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 通知Service
 */
@Service
public class NotificationService {
    
    @Autowired
    private NotificationRepository notificationRepository;
    
    @Autowired
    private UserRepository userRepository;

    @Autowired
    private SimpMessagingTemplate messagingTemplate;
    
    /**
     * 创建通知（通用方法）
     */
    @Transactional
    public Notification createNotification(Long userId, Long senderId, String type,
                                          String title, String content, Long relatedId,
                                          String relatedType, String link) {
        User sender = null;
        if (senderId != null) {
            sender = userRepository.findById(senderId).orElse(null);
        }
        
        Notification notification = new Notification();
        notification.setUserId(userId);
        notification.setSenderId(senderId);
        notification.setType(type);
        notification.setTitle(title);
        notification.setContent(content);
        notification.setRelatedId(relatedId);
        notification.setRelatedType(relatedType);
        notification.setLink(link);
        notification.setIsRead(false);
        
        if (sender != null) {
            notification.setSenderName(sender.getNickname());
            notification.setSenderAvatar(sender.getAvatar());
        }
        
        return notificationRepository.save(notification);
    }
    
    /**
     * 创建评论通知
     */
    public Notification createCommentNotification(Long userId, Long senderId, 
                                                  Long commentId, String content) {
        return createNotification(
            userId, senderId, "comment",
            "新评论", content, commentId, "comment",
            "/comment/" + commentId
        );
    }
    
    /**
     * 创建评论回复通知
     */
    public Notification createCommentReplyNotification(Long userId, Long senderId,
                                                       Long commentId, String content) {
        return createNotification(
            userId, senderId, "comment",
            "评论回复", content, commentId, "comment",
            "/comment/" + commentId
        );
    }
    
    /**
     * 创建评论点赞通知
     */
    public Notification createCommentLikeNotification(Long userId, Long senderId,
                                                      Long commentId) {
        return createNotification(
            userId, senderId, "like",
            "点赞", "赞了你的评论", commentId, "comment",
            "/comment/" + commentId
        );
    }
    
    /**
     * 创建好友请求通知
     */
    public Notification createFriendRequestNotification(Long userId, Long senderId) {
        Notification notification = createNotification(
            userId, senderId, "friend_request",
            "好友请求", "请求添加你为好友", null, null,
            "/friends/requests"
        );
        // 推送未读通知数更新
        sendUnreadCount(userId);
        return notification;
    }
    
    /**
     * 创建好友接受通知
     */
    public Notification createFriendAcceptNotification(Long userId, Long senderId) {
        Notification notification = createNotification(
            userId, senderId, "friend_accept",
            "好友已接受", "接受了你的好友请求", null, null,
            "/friends"
        );
        // 推送未读通知数更新
        sendUnreadCount(userId);
        return notification;
    }

    /**
     * 通过WebSocket推送未读通知数
     */
    private void sendUnreadCount(Long userId) {
        try {
            long count = getUnreadCount(userId);
            String topic = "/topic/user." + userId + ".notifications";
            java.util.Map<String, Object> payload = new java.util.HashMap<>();
            payload.put("type", "notification_unread_count");
            payload.put("unreadCount", count);
            messagingTemplate.convertAndSend(topic, payload);
        } catch (Exception ignored) {}
    }
    
    /**
     * 创建动态点赞通知
     */
    public Notification createMomentLikeNotification(Long userId, Long senderId,
                                                     Long momentId) {
        return createNotification(
            userId, senderId, "moment_like",
            "点赞", "赞了你的动态", momentId, "moment",
            "/moment/" + momentId
        );
    }
    
    /**
     * 创建动态评论通知
     */
    public Notification createMomentCommentNotification(Long userId, Long senderId,
                                                        Long momentId, String content) {
        return createNotification(
            userId, senderId, "moment_comment",
            "动态评论", content, momentId, "moment",
            "/moment/" + momentId
        );
    }
    
    /**
     * 获取用户的通知
     */
    public Page<Notification> getNotifications(Long userId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return notificationRepository.findByUserIdOrderByCreateTimeDesc(userId, pageable);
    }
    
    /**
     * 获取未读通知
     */
    public List<Notification> getUnreadNotifications(Long userId) {
        return notificationRepository.findByUserIdAndIsReadFalseOrderByCreateTimeDesc(userId);
    }
    
    /**
     * 获取未读通知数
     */
    public long getUnreadCount(Long userId) {
        return notificationRepository.countByUserIdAndIsReadFalse(userId);
    }
    
    /**
     * 标记通知为已读
     */
    @Transactional
    public void markAsRead(Long notificationId) {
        notificationRepository.markAsRead(notificationId, LocalDateTime.now());
    }
    
    /**
     * 批量标记通知为已读
     */
    @Transactional
    public void markBatchAsRead(List<Long> notificationIds) {
        if (notificationIds != null && !notificationIds.isEmpty()) {
            notificationRepository.markBatchAsRead(notificationIds, LocalDateTime.now());
        }
    }
    
    /**
     * 标记所有通知为已读
     */
    @Transactional
    public void markAllAsRead(Long userId) {
        notificationRepository.markAllAsRead(userId, LocalDateTime.now());
    }
    
    /**
     * 删除旧通知（30天前）
     */
    @Transactional
    public void deleteOldNotifications() {
        LocalDateTime beforeDate = LocalDateTime.now().minusDays(30);
        notificationRepository.deleteOldNotifications(beforeDate);
    }
}

