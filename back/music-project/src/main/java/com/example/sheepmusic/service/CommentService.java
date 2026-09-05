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

        // 收集所有层级的回复（回复的回复也要一并删除）
        List<Long> allIds = new java.util.ArrayList<>();
        allIds.add(commentId);
        List<SongComment> allReplies = new java.util.ArrayList<>();
        List<Long> frontier = new java.util.ArrayList<>();
        frontier.add(commentId);
        while (!frontier.isEmpty()) {
            List<SongComment> children = commentRepository.findByParentIdIn(frontier);
            if (children.isEmpty()) {
                break;
            }
            allReplies.addAll(children);
            frontier = children.stream().map(SongComment::getId)
                .collect(java.util.stream.Collectors.toList());
            allIds.addAll(frontier);
        }

        // 删除评论及其所有回复
        commentRepository.delete(comment);
        commentRepository.deleteAll(allReplies);

        // 删除所有相关点赞记录（含回复的点赞）
        commentLikeRepository.findByCommentIdIn(allIds)
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
            // 取消点赞（计数用原子更新，避免并发丢失）
            commentLikeRepository.deleteByCommentIdAndUserId(commentId, userId);
            commentRepository.decrementLikeCount(commentId);
            return false;
        } else {
            // 点赞
            CommentLike like = new CommentLike();
            like.setCommentId(commentId);
            like.setUserId(userId);
            commentLikeRepository.save(like);

            commentRepository.incrementLikeCount(commentId);
            
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

