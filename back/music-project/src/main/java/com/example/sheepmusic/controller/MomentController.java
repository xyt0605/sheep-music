package com.example.sheepmusic.controller;

import com.example.sheepmusic.common.Result;
import com.example.sheepmusic.dto.MomentCommentRequest;
import com.example.sheepmusic.dto.MomentRequest;
import com.example.sheepmusic.entity.MomentComment;
import com.example.sheepmusic.entity.UserMoment;
import com.example.sheepmusic.service.MomentService;
import com.example.sheepmusic.utils.JwtUtil;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;

/**
 * 动态Controller
 */
@Api(tags = "动态管理")
@RestController
@RequestMapping("/moment")
@CrossOrigin
public class MomentController {
    
    @Autowired
    private MomentService momentService;
    
    @Autowired
    private JwtUtil jwtUtil;
    
    /**
     * 发布动态
     */
    @ApiOperation("发布动态")
    @PostMapping
    public Result<UserMoment> publishMoment(@Valid @RequestBody MomentRequest request,
                                            HttpServletRequest httpRequest) {
        try {
            Long userId = jwtUtil.getUserIdFromRequest(httpRequest);
            
            UserMoment moment = momentService.publishMoment(
                userId,
                request.getType(),
                request.getContent(),
                request.getRelatedId(),
                request.getImages(),
                request.getVisibility()
            );
            
            return Result.success("发布成功", moment);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }
    
    /**
     * 获取好友动态
     */
    @ApiOperation("获取好友动态")
    @GetMapping("/friends")
    public Result<Page<UserMoment>> getFriendsMoments(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            HttpServletRequest request) {
        try {
            Long userId = jwtUtil.getUserIdFromRequest(request);
            Page<UserMoment> moments = momentService.getFriendsMoments(userId, page, size);
            return Result.success("查询成功", moments);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }
    
    /**
     * 获取公开动态
     */
    @ApiOperation("获取公开动态")
    @GetMapping("/public")
    public Result<Page<UserMoment>> getPublicMoments(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        try {
            Page<UserMoment> moments = momentService.getPublicMoments(page, size);
            return Result.success("查询成功", moments);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }
    
    /**
     * 获取用户的动态
     */
    @ApiOperation("获取用户的动态")
    @GetMapping("/user/{userId}")
    public Result<Page<UserMoment>> getUserMoments(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        try {
            Page<UserMoment> moments = momentService.getUserMoments(userId, page, size);
            return Result.success("查询成功", moments);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }
    
    /**
     * 点赞/取消点赞动态
     */
    @ApiOperation("点赞/取消点赞动态")
    @PostMapping("/{momentId}/like")
    public Result<Boolean> toggleLike(@PathVariable Long momentId,
                                      HttpServletRequest request) {
        try {
            Long userId = jwtUtil.getUserIdFromRequest(request);
            boolean liked = momentService.toggleLike(momentId, userId);
            
            return Result.success(liked ? "点赞成功" : "取消点赞", liked);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }
    
    /**
     * 评论动态
     */
    @ApiOperation("评论动态")
    @PostMapping("/comment")
    public Result<MomentComment> commentMoment(@Valid @RequestBody MomentCommentRequest request,
                                               HttpServletRequest httpRequest) {
        try {
            Long userId = jwtUtil.getUserIdFromRequest(httpRequest);
            
            MomentComment comment = momentService.commentMoment(
                request.getMomentId(),
                userId,
                request.getContent(),
                request.getParentId()
            );
            
            return Result.success("评论成功", comment);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }
    
    /**
     * 获取动态评论
     */
    @ApiOperation("获取动态评论")
    @GetMapping("/{momentId}/comments")
    public Result<Page<MomentComment>> getMomentComments(
            @PathVariable Long momentId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        try {
            Page<MomentComment> comments = momentService.getMomentComments(momentId, page, size);
            return Result.success("查询成功", comments);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }
    
    /**
     * 删除动态
     */
    @ApiOperation("删除动态")
    @DeleteMapping("/{momentId}")
    public Result<Void> deleteMoment(@PathVariable Long momentId,
                                     HttpServletRequest request) {
        try {
            Long userId = jwtUtil.getUserIdFromRequest(request);
            momentService.deleteMoment(momentId, userId);
            
            Result<Void> result = Result.success();
            result.setMessage("删除成功");
            return result;
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }
    
    /**
     * 检查是否点赞
     */
    @ApiOperation("检查是否点赞")
    @GetMapping("/{momentId}/liked")
    public Result<Boolean> hasLiked(@PathVariable Long momentId,
                                    HttpServletRequest request) {
        try {
            Long userId = jwtUtil.getUserIdFromRequest(request);
            boolean liked = momentService.hasLiked(momentId, userId);
            return Result.success("查询成功", liked);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }
}

