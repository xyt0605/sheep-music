package com.example.sheepmusic.service;

import com.example.sheepmusic.dto.ConversationVO;
import com.example.sheepmusic.entity.Conversation;
import com.example.sheepmusic.entity.User;
import com.example.sheepmusic.repository.ConversationRepository;
import com.example.sheepmusic.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 会话Service
 */
@Service
public class ConversationService {
    
    @Autowired
    private ConversationRepository conversationRepository;
    
    @Autowired
    private UserRepository userRepository;
    
    /**
     * 创建或更新会话
     */
    @Transactional
    public Conversation createOrUpdateConversation(Long userId, Long friendId) {
        User friend = userRepository.findById(friendId)
            .orElseThrow(() -> new RuntimeException("好友不存在"));
        
        Conversation conversation = conversationRepository
            .findByUserIdAndFriendId(userId, friendId)
            .orElse(new Conversation());
        
        if (conversation.getId() == null) {
            conversation.setUserId(userId);
            conversation.setFriendId(friendId);
            conversation.setUnreadCount(0);
            conversation.setFriendName(friend.getNickname());
            conversation.setFriendAvatar(friend.getAvatar());
        }
        
        return conversationRepository.save(conversation);
    }
    
    /**
     * 更新会话（收到新消息时）
     */
    @Transactional
    public void updateConversationOnNewMessage(Long userId, Long friendId, 
                                               String message, LocalDateTime time) {
        Conversation conversation = conversationRepository
            .findByUserIdAndFriendId(userId, friendId)
            .orElse(createOrUpdateConversation(userId, friendId));
        
        conversation.setLastMessageContent(message);
        conversation.setLastMessageTime(time);
        conversation.setUnreadCount(conversation.getUnreadCount() + 1);
        conversationRepository.save(conversation);
    }
    
    /**
     * 更新发送者的会话（发送消息时，仅更新最后一条消息，不增加未读数）
     */
    @Transactional
    public void updateSenderConversation(Long userId, Long friendId, 
                                         String message, LocalDateTime time) {
        Conversation conversation = conversationRepository
            .findByUserIdAndFriendId(userId, friendId)
            .orElse(createOrUpdateConversation(userId, friendId));
        
        conversation.setLastMessageContent(message);
        conversation.setLastMessageTime(time);
        // 不增加 unreadCount，因为发送者自己发的消息不需要未读提示
        conversationRepository.save(conversation);
    }
    
    /**
     * 清空未读数
     */
    @Transactional
    public void clearUnreadCount(Long userId, Long friendId) {
        conversationRepository.clearUnreadCount(userId, friendId);
    }
    
    /**
     * 获取会话列表
     */
    public List<ConversationVO> getConversations(Long userId) {
        List<Conversation> conversations = conversationRepository
            .findByUserIdOrderByLastMessageTimeDesc(userId);
        
        return conversations.stream().map(conv -> {
            ConversationVO vo = new ConversationVO();
            vo.setId(conv.getId());
            vo.setFriendId(conv.getFriendId());
            
            // 从 User 表获取最新的好友信息（确保昵称和头像是最新的）
            User friend = userRepository.findById(conv.getFriendId()).orElse(null);
            if (friend != null) {
                vo.setFriendName(friend.getNickname());
                vo.setFriendAvatar(friend.getAvatar());
            } else {
                // 如果用户不存在（已删除等），使用缓存的信息
                vo.setFriendName(conv.getFriendName());
                vo.setFriendAvatar(conv.getFriendAvatar());
            }
            
            vo.setLastMessage(conv.getLastMessageContent());
            vo.setLastMessageTime(conv.getLastMessageTime());
            vo.setUnreadCount(conv.getUnreadCount());
            return vo;
        }).collect(Collectors.toList());
    }
    
    /**
     * 获取总未读消息数
     */
    public long getTotalUnreadCount(Long userId) {
        Long count = conversationRepository.getTotalUnreadCount(userId);
        return count != null ? count : 0L;
    }
}

