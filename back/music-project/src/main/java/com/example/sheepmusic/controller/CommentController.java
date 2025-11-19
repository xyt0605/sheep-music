package com.example.sheepmusic.controller;

import com.example.sheepmusic.common.Result;
import com.example.sheepmusic.dto.CommentRequest;
import com.example.sheepmusic.entity.SongComment;
import com.example.sheepmusic.service.CommentService;
import com.example.sheepmusic.utils.JwtUtil;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import java.util.List;

/**
 * 评论Controller
 */
@Api(tags = "评论管理")
@RestController
@RequestMapping("/comment")
@CrossOrigin
public class CommentController {
    
    @Autowired
    private CommentService commentService;
    
    @Autowired
    private JwtUtil jwtUtil;
    
    /**
     * 发表评论
     */
    @ApiOperation("发表评论")
    @PostMapping
    public Result<SongComment> addComment(@Valid @RequestBody CommentRequest request,
                                          HttpServletRequest httpRequest) {
        try {
            Long userId = jwtUtil.getUserIdFromRequest(httpRequest);
            
            SongComment comment = commentService.addComment(
                request.getSongId(),
                userId,
                request.getContent(),
                request.getRating(),
                request.getParentId()
            );
            
            return Result.success("评论成功", comment);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }
    
    /**
     * 获取歌曲评论
     */
    @ApiOperation("获取歌曲评论")
    @GetMapping("/song/{songId}")
    public Result<Page<SongComment>> getSongComments(
            @PathVariable Long songId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "latest") String sort) {
        try {
            Page<SongComment> comments = commentService.getSongComments(songId, page, size, sort);
            return Result.success("查询成功", comments);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }
    
    /**
     * 获取评论的回复
     */
    @ApiOperation("获取评论的回复")
    @GetMapping("/{commentId}/replies")
    public Result<List<SongComment>> getCommentReplies(@PathVariable Long commentId) {
        try {
            List<SongComment> replies = commentService.getCommentReplies(commentId);
            return Result.success("查询成功", replies);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }
    
    /**
     * 删除评论
     */
    @ApiOperation("删除评论")
    @DeleteMapping("/{commentId}")
    public Result<Void> deleteComment(@PathVariable Long commentId,
                                      HttpServletRequest request) {
        try {
            Long userId = jwtUtil.getUserIdFromRequest(request);
            commentService.deleteComment(commentId, userId);
            
            Result<Void> result = Result.success();
            result.setMessage("删除成功");
            return result;
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }
    
    /**
     * 点赞/取消点赞评论
     */
    @ApiOperation("点赞/取消点赞评论")
    @PostMapping("/{commentId}/like")
    public Result<Boolean> toggleLike(@PathVariable Long commentId,
                                      HttpServletRequest request) {
        try {
            Long userId = jwtUtil.getUserIdFromRequest(request);
            boolean liked = commentService.toggleLike(commentId, userId);
            
            return Result.success(liked ? "点赞成功" : "取消点赞", liked);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }
    
    /**
     * 获取用户的评论
     */
    @ApiOperation("获取用户的评论")
    @GetMapping("/user/{userId}")
    public Result<Page<SongComment>> getUserComments(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        try {
            Page<SongComment> comments = commentService.getUserComments(userId, page, size);
            return Result.success("查询成功", comments);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }
    
    /**
     * 统计歌曲评论数
     */
    @ApiOperation("统计歌曲评论数")
    @GetMapping("/count/{songId}")
    public Result<Long> countBySongId(@PathVariable Long songId) {
        try {
            long count = commentService.countBySongId(songId);
            return Result.success("查询成功", count);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }
    
    /**
     * 检查是否点赞
     */
    @ApiOperation("检查是否点赞")
    @GetMapping("/{commentId}/liked")
    public Result<Boolean> hasLiked(@PathVariable Long commentId,
                                    HttpServletRequest request) {
        try {
            Long userId = jwtUtil.getUserIdFromRequest(request);
            boolean liked = commentService.hasLiked(commentId, userId);
            return Result.success("查询成功", liked);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }
}

