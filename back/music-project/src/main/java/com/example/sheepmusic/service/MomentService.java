package com.example.sheepmusic.service;

import com.example.sheepmusic.entity.*;
import com.example.sheepmusic.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 动态Service
 */
@Service
public class MomentService {
    
    @Autowired
    private UserMomentRepository momentRepository;
    
    @Autowired
    private MomentLikeRepository momentLikeRepository;
    
    @Autowired
    private MomentCommentRepository momentCommentRepository;
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private SongRepository songRepository;
    
    @Autowired
    private PlaylistRepository playlistRepository;
    
    @Autowired
    private FriendshipService friendshipService;
    
    @Autowired
    private NotificationService notificationService;
    
    /**
     * 发布动态
     */
    @Transactional
    public UserMoment publishMoment(Long userId, String type, String content, 
                                    Long relatedId, String images, String visibility) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new RuntimeException("用户不存在"));
        
        UserMoment moment = new UserMoment();
        moment.setUserId(userId);
        moment.setType(type);
        moment.setContent(content);
        moment.setRelatedId(relatedId);
        moment.setImages(images);
        moment.setVisibility(visibility);
        moment.setLikeCount(0);
        moment.setCommentCount(0);
        
        // 冗余用户信息
        moment.setUsername(user.getNickname());
        moment.setUserAvatar(user.getAvatar());
        
        // 冗余关联对象信息
        if (relatedId != null) {
            if ("song".equals(type)) {
                Song song = songRepository.findById(relatedId).orElse(null);
                if (song != null) {
                    moment.setRelatedTitle(song.getTitle());
                    moment.setRelatedCover(song.getCover());
                }
            } else if ("playlist".equals(type)) {
                Playlist playlist = playlistRepository.findById(relatedId).orElse(null);
                if (playlist != null) {
                    moment.setRelatedTitle(playlist.getName());
                    moment.setRelatedCover(playlist.getCover());
                }
            }
        }
        
        return momentRepository.save(moment);
    }
    
    /**
     * 获取好友动态
     */
    public Page<UserMoment> getFriendsMoments(Long userId, int page, int size) {
        List<Friendship> friends = friendshipService.getFriendList(userId);
        List<Long> friendIds = friends.stream()
            .map(Friendship::getFriendId)
            .collect(java.util.stream.Collectors.toList());
        
        // 加上自己
        friendIds.add(userId);
        
        Pageable pageable = PageRequest.of(page, size);
        return momentRepository.findFriendsMoments(friendIds, pageable);
    }
    
    /**
     * 获取公开动态
     */
    public Page<UserMoment> getPublicMoments(int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return momentRepository.findByVisibilityOrderByCreateTimeDesc("public", pageable);
    }
    
    /**
     * 获取用户的动态
     */
    public Page<UserMoment> getUserMoments(Long userId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return momentRepository.findByUserIdOrderByCreateTimeDesc(userId, pageable);
    }
    
    /**
     * 点赞/取消点赞动态
     */
    @Transactional
    public boolean toggleLike(Long momentId, Long userId) {
        UserMoment moment = momentRepository.findById(momentId)
            .orElseThrow(() -> new RuntimeException("动态不存在"));
        
        boolean exists = momentLikeRepository.existsByMomentIdAndUserId(momentId, userId);
        
        if (exists) {
            // 取消点赞
            momentLikeRepository.deleteByMomentIdAndUserId(momentId, userId);
            moment.setLikeCount(Math.max(0, moment.getLikeCount() - 1));
            momentRepository.save(moment);
            return false;
        } else {
            // 点赞
            MomentLike like = new MomentLike();
            like.setMomentId(momentId);
            like.setUserId(userId);
            momentLikeRepository.save(like);
            
            moment.setLikeCount(moment.getLikeCount() + 1);
            momentRepository.save(moment);
            
            // 创建通知
            if (!moment.getUserId().equals(userId)) {
                notificationService.createMomentLikeNotification(
                    moment.getUserId(), userId, momentId);
            }
            
            return true;
        }
    }
    
    /**
     * 评论动态
     */
    @Transactional
    public MomentComment commentMoment(Long momentId, Long userId, 
                                       String content, Long parentId) {
        UserMoment moment = momentRepository.findById(momentId)
            .orElseThrow(() -> new RuntimeException("动态不存在"));
        
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new RuntimeException("用户不存在"));
        
        MomentComment comment = new MomentComment();
        comment.setMomentId(momentId);
        comment.setUserId(userId);
        comment.setContent(content);
        comment.setParentId(parentId);
        
        // 冗余用户信息
        comment.setUsername(user.getNickname());
        comment.setUserAvatar(user.getAvatar());
        
        MomentComment saved = momentCommentRepository.save(comment);
        
        // 更新动态评论数
        moment.setCommentCount(moment.getCommentCount() + 1);
        momentRepository.save(moment);
        
        // 创建通知
        if (!moment.getUserId().equals(userId)) {
            notificationService.createMomentCommentNotification(
                moment.getUserId(), userId, momentId, content);
        }
        
        return saved;
    }
    
    /**
     * 获取动态评论
     */
    public Page<MomentComment> getMomentComments(Long momentId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return momentCommentRepository.findByMomentIdAndParentIdIsNullOrderByCreateTimeDesc(
            momentId, pageable);
    }
    
    /**
     * 删除动态
     */
    @Transactional
    public void deleteMoment(Long momentId, Long userId) {
        UserMoment moment = momentRepository.findById(momentId)
            .orElseThrow(() -> new RuntimeException("动态不存在"));
        
        if (!moment.getUserId().equals(userId)) {
            throw new RuntimeException("无权删除该动态");
        }
        
        // 删除动态及相关数据
        momentRepository.delete(moment);
        
        // 删除点赞记录
        List<MomentLike> likes = momentLikeRepository.findAll().stream()
            .filter(like -> like.getMomentId().equals(momentId))
            .collect(java.util.stream.Collectors.toList());
        momentLikeRepository.deleteAll(likes);
        
        // 删除评论
        List<MomentComment> comments = momentCommentRepository.findByMomentIdOrderByCreateTimeDesc(momentId);
        momentCommentRepository.deleteAll(comments);
    }
    
    /**
     * 检查用户是否点赞了动态
     */
    public boolean hasLiked(Long momentId, Long userId) {
        return momentLikeRepository.existsByMomentIdAndUserId(momentId, userId);
    }
}

