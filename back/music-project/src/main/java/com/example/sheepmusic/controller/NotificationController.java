package com.example.sheepmusic.controller;

import com.example.sheepmusic.common.Result;
import com.example.sheepmusic.entity.Notification;
import com.example.sheepmusic.service.NotificationService;
import com.example.sheepmusic.utils.JwtUtil;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

/**
 * 通知Controller
 */
@Api(tags = "通知管理")
@RestController
@RequestMapping("/notification")
@CrossOrigin
public class NotificationController {
    
    @Autowired
    private NotificationService notificationService;
    
    @Autowired
    private JwtUtil jwtUtil;
    
    /**
     * 获取通知列表
     */
    @ApiOperation("获取通知列表")
    @GetMapping("/list")
    public Result<Page<Notification>> getNotifications(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            HttpServletRequest request) {
        try {
            Long userId = jwtUtil.getUserIdFromRequest(request);
            Page<Notification> notifications = notificationService.getNotifications(userId, page, size);
            return Result.success("查询成功", notifications);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }
    
    /**
     * 获取未读通知
     */
    @ApiOperation("获取未读通知")
    @GetMapping("/unread")
    public Result<List<Notification>> getUnreadNotifications(HttpServletRequest request) {
        try {
            Long userId = jwtUtil.getUserIdFromRequest(request);
            List<Notification> notifications = notificationService.getUnreadNotifications(userId);
            return Result.success("查询成功", notifications);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }
    
    /**
     * 获取未读通知数
     */
    @ApiOperation("获取未读通知数")
    @GetMapping("/unread-count")
    public Result<Long> getUnreadCount(HttpServletRequest request) {
        try {
            Long userId = jwtUtil.getUserIdFromRequest(request);
            long count = notificationService.getUnreadCount(userId);
            return Result.success("查询成功", count);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }
    
    /**
     * 标记通知为已读
     */
    @ApiOperation("标记通知为已读")
    @PutMapping("/read/{notificationId}")
    public Result<Void> markAsRead(@PathVariable Long notificationId) {
        try {
            notificationService.markAsRead(notificationId);
            
            Result<Void> result = Result.success();
            result.setMessage("操作成功");
            return result;
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }
    
    /**
     * 批量标记通知为已读
     */
    @ApiOperation("批量标记通知为已读")
    @PutMapping("/read/batch")
    public Result<Void> markBatchAsRead(@RequestBody List<Long> notificationIds) {
        try {
            notificationService.markBatchAsRead(notificationIds);
            
            Result<Void> result = Result.success();
            result.setMessage("操作成功");
            return result;
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }
    
    /**
     * 标记所有通知为已读
     */
    @ApiOperation("标记所有通知为已读")
    @PutMapping("/read/all")
    public Result<Void> markAllAsRead(HttpServletRequest request) {
        try {
            Long userId = jwtUtil.getUserIdFromRequest(request);
            notificationService.markAllAsRead(userId);
            
            Result<Void> result = Result.success();
            result.setMessage("操作成功");
            return result;
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }
}

