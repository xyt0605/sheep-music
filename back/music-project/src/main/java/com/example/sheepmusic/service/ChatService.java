package com.example.sheepmusic.service;

import com.example.sheepmusic.dto.ConversationVO;
import com.example.sheepmusic.entity.ChatMessage;
import com.example.sheepmusic.entity.User;
import com.example.sheepmusic.repository.ChatMessageRepository;
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
 * 聊天Service
 */
@Service
public class ChatService {
    
    @Autowired
    private ChatMessageRepository chatMessageRepository;
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private ConversationService conversationService;
    
    @Autowired
    private FriendshipService friendshipService;

    @Autowired
    private SimpMessagingTemplate messagingTemplate;
    
    /**
     * 发送消息
     */
    @Transactional
    public ChatMessage sendMessage(Long senderId, Long receiverId, String type, 
                                   String content, String extra) {
        // 验证是否为好友
        if (!friendshipService.areFriends(senderId, receiverId)) {
            throw new RuntimeException("只能给好友发送消息");
        }
        
        User sender = userRepository.findById(senderId)
            .orElseThrow(() -> new RuntimeException("发送者不存在"));
        
        ChatMessage message = new ChatMessage();
        message.setSenderId(senderId);
        message.setReceiverId(receiverId);
        message.setType(type);
        message.setContent(content);
        message.setExtra(extra);
        message.setIsRead(false);
        message.setIsRecalled(false);
        
        // 冗余发送者信息
        message.setSenderName(sender.getNickname());
        message.setSenderAvatar(sender.getAvatar());
        
        ChatMessage savedMessage = chatMessageRepository.save(message);

        // 生成会话显示文本
        String displayContent = content;
        if ("song".equals(type)) {
            displayContent = "[分享了歌曲]";
        } else if ("playlist".equals(type)) {
            displayContent = "[分享了歌单]";
        }

        // 更新会话信息
        conversationService.updateConversationOnNewMessage(
            receiverId, senderId, displayContent, savedMessage.getCreateTime());

        // 通过WebSocket推送消息给接收者与发送者（用于多端同步）
        try {
            String receiverTopic = "/topic/user." + receiverId + ".chat";
            String senderTopic = "/topic/user." + senderId + ".chat";
            messagingTemplate.convertAndSend(receiverTopic, savedMessage);
            messagingTemplate.convertAndSend(senderTopic, savedMessage);
        } catch (Exception ignored) {}

        return savedMessage;
    }
    
    /**
     * 获取聊天记录
     */
    public Page<ChatMessage> getChatHistory(Long userId, Long friendId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return chatMessageRepository.findChatHistory(userId, friendId, pageable);
    }
    
    /**
     * 获取会话列表
     */
    public List<ConversationVO> getConversations(Long userId) {
        return conversationService.getConversations(userId);
    }
    
    /**
     * 标记消息为已读
     */
    @Transactional
    public void markAsRead(Long messageId, Long userId) {
        ChatMessage message = chatMessageRepository.findById(messageId)
            .orElseThrow(() -> new RuntimeException("消息不存在"));
        
        if (!message.getReceiverId().equals(userId)) {
            throw new RuntimeException("无权操作该消息");
        }
        
        if (!message.getIsRead()) {
            message.setIsRead(true);
            message.setReadTime(LocalDateTime.now());
            chatMessageRepository.save(message);
        }
    }
    
    /**
     * 批量标记消息为已读
     */
    @Transactional
    public void markBatchAsRead(List<Long> messageIds) {
        if (messageIds != null && !messageIds.isEmpty()) {
            chatMessageRepository.markAsRead(messageIds, LocalDateTime.now());
        }
    }
    
    /**
     * 标记与某人的所有未读消息为已读
     */
    @Transactional
    public void markAllAsReadFrom(Long senderId, Long receiverId) {
        chatMessageRepository.markAllAsReadBetween(senderId, receiverId, LocalDateTime.now());
        
        // 清空会话未读数
        conversationService.clearUnreadCount(receiverId, senderId);
    }
    
    /**
     * 撤回消息
     */
    @Transactional
    public void recallMessage(Long messageId, Long userId) {
        ChatMessage message = chatMessageRepository.findById(messageId)
            .orElseThrow(() -> new RuntimeException("消息不存在"));
        
        if (!message.getSenderId().equals(userId)) {
            throw new RuntimeException("只能撤回自己发送的消息");
        }
        
        // 检查是否在2分钟内
        if (message.getCreateTime().plusMinutes(2).isBefore(LocalDateTime.now())) {
            throw new RuntimeException("只能撤回2分钟内的消息");
        }
        
        message.setIsRecalled(true);
        message.setContent("[消息已撤回]");
        chatMessageRepository.save(message);
    }
    
    /**
     * 获取未读消息数
     */
    public long getUnreadCount(Long userId) {
        return chatMessageRepository.countByReceiverIdAndIsReadFalse(userId);
    }
    
    /**
     * 获取来自某人的未读消息数
     */
    public long getUnreadCountFrom(Long senderId, Long receiverId) {
        return chatMessageRepository.countBySenderIdAndReceiverIdAndIsReadFalse(
            senderId, receiverId);
    }
}

