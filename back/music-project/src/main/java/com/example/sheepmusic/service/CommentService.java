package com.example.sheepmusic.service;

import com.example.sheepmusic.entity.CommentLike;
import com.example.sheepmusic.entity.SongComment;
import com.example.sheepmusic.entity.User;
import com.example.sheepmusic.repository.CommentLikeRepository;
import com.example.sheepmusic.repository.SongCommentRepository;
import com.example.sheepmusic.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 评论Service
 */
@Service
public class CommentService {
    
    @Autowired
    private SongCommentRepository commentRepository;
    
    @Autowired
    private CommentLikeRepository commentLikeRepository;
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private NotificationService notificationService;
    
    /**
     * 发表评论
     */
    @Transactional
    public SongComment addComment(Long songId, Long userId, String content, 
                                  Integer rating, Long parentId) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new RuntimeException("用户不存在"));
        
        SongComment comment = new SongComment();
        comment.setSongId(songId);
        comment.setUserId(userId);
        comment.setContent(content);
        comment.setRating(rating);
        comment.setParentId(parentId);
        comment.setLikeCount(0);
        comment.setPinned(false);
        
        // 冗余用户信息
        comment.setUsername(user.getNickname());
        comment.setUserAvatar(user.getAvatar());
        
        SongComment savedComment = commentRepository.save(comment);
        
        // 如果是回复评论，创建通知
        if (parentId != null) {
            SongComment parentComment = commentRepository.findById(parentId).orElse(null);
            if (parentComment != null && !parentComment.getUserId().equals(userId)) {
                notificationService.createCommentReplyNotification(
                    parentComment.getUserId(), userId, comment.getId(), content);
            }
        }
        
        return savedComment;
    }
    
    /**
     * 获取歌曲评论（分页）
     */
    public Page<SongComment> getSongComments(Long songId, int page, int size, String sort) {
        Pageable pageable = PageRequest.of(page, size);
        
        if ("hot".equals(sort)) {
            return commentRepository.findHotComments(songId, pageable);
        } else {
            return commentRepository.findBySongIdAndParentIdIsNullOrderByPinnedDescCreateTimeDesc(
                songId, pageable);
        }
    }
    
    /**
     * 获取评论的回复
     */
    public List<SongComment> getCommentReplies(Long commentId) {
        return commentRepository.findByParentIdOrderByCreateTimeAsc(commentId);
    }
    
    /**
     * 删除评论
     */
    @Transactional
    public void deleteComment(Long commentId, Long userId) {
        SongComment comment = commentRepository.findById(commentId)
            .orElseThrow(() -> new RuntimeException("评论不存在"));
        
        // 只能删除自己的评论
        if (!comment.getUserId().equals(userId)) {
            throw new RuntimeException("无权删除该评论");
        }
        
        // 删除评论及其所有回复
        commentRepository.delete(comment);
        List<SongComment> replies = commentRepository.findByParentIdOrderByCreateTimeAsc(commentId);
        commentRepository.deleteAll(replies);
        
        // 删除相关点赞记录
        commentLikeRepository.findByCommentId(commentId)
            .forEach(like -> commentLikeRepository.delete(like));
    }
    
    /**
     * 点赞/取消点赞评论
     */
    @Transactional
    public boolean toggleLike(Long commentId, Long userId) {
        SongComment comment = commentRepository.findById(commentId)
            .orElseThrow(() -> new RuntimeException("评论不存在"));
        
        // 查询是否已点赞
        boolean exists = commentLikeRepository.existsByCommentIdAndUserId(commentId, userId);
        
        if (exists) {
            // 取消点赞
            commentLikeRepository.deleteByCommentIdAndUserId(commentId, userId);
            comment.setLikeCount(Math.max(0, comment.getLikeCount() - 1));
            commentRepository.save(comment);
            return false;
        } else {
            // 点赞
            CommentLike like = new CommentLike();
            like.setCommentId(commentId);
            like.setUserId(userId);
            commentLikeRepository.save(like);
            
            comment.setLikeCount(comment.getLikeCount() + 1);
            commentRepository.save(comment);
            
            // 创建通知（不给自己发通知）
            if (!comment.getUserId().equals(userId)) {
                notificationService.createCommentLikeNotification(
                    comment.getUserId(), userId, commentId);
            }
            
            return true;
        }
    }
    
    /**
     * 获取用户的评论
     */
    public Page<SongComment> getUserComments(Long userId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return commentRepository.findByUserIdOrderByCreateTimeDesc(userId, pageable);
    }
    
    /**
     * 统计歌曲评论数
     */
    public long countBySongId(Long songId) {
        return commentRepository.countBySongId(songId);
    }
    
    /**
     * 检查用户是否点赞了评论
     */
    public boolean hasLiked(Long commentId, Long userId) {
        return commentLikeRepository.existsByCommentIdAndUserId(commentId, userId);
    }
}

