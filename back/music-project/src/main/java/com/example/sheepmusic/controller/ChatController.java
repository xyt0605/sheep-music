package com.example.sheepmusic.controller;

import com.example.sheepmusic.common.Result;
import com.example.sheepmusic.dto.ChatMessageRequest;
import com.example.sheepmusic.dto.ConversationVO;
import com.example.sheepmusic.entity.ChatMessage;
import com.example.sheepmusic.service.ChatService;
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
 * 聊天Controller
 */
@Api(tags = "聊天管理")
@RestController
@RequestMapping("/chat")
@CrossOrigin
public class ChatController {
    
    @Autowired
    private ChatService chatService;
    
    @Autowired
    private JwtUtil jwtUtil;
    
    /**
     * 发送消息
     */
    @ApiOperation("发送消息")
    @PostMapping("/send")
    public Result<ChatMessage> sendMessage(@Valid @RequestBody ChatMessageRequest request,
                                          HttpServletRequest httpRequest) {
        try {
            Long userId = jwtUtil.getUserIdFromRequest(httpRequest);
            
            ChatMessage message = chatService.sendMessage(
                userId,
                request.getReceiverId(),
                request.getType(),
                request.getContent(),
                request.getExtra()
            );
            
            return Result.success("发送成功", message);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }
    
    /**
     * 获取聊天记录
     */
    @ApiOperation("获取聊天记录")
    @GetMapping("/history/{friendId}")
    public Result<Page<ChatMessage>> getChatHistory(
            @PathVariable Long friendId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size,
            HttpServletRequest request) {
        try {
            Long userId = jwtUtil.getUserIdFromRequest(request);
            Page<ChatMessage> messages = chatService.getChatHistory(userId, friendId, page, size);
            return Result.success("查询成功", messages);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }
    
    /**
     * 获取会话列表
     */
    @ApiOperation("获取会话列表")
    @GetMapping("/conversations")
    public Result<List<ConversationVO>> getConversations(HttpServletRequest request) {
        try {
            Long userId = jwtUtil.getUserIdFromRequest(request);
            List<ConversationVO> conversations = chatService.getConversations(userId);
            return Result.success("查询成功", conversations);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }
    
    /**
     * 标记消息为已读
     */
    @ApiOperation("标记消息为已读")
    @PutMapping("/read/{messageId}")
    public Result<Void> markAsRead(@PathVariable Long messageId,
                                   HttpServletRequest request) {
        try {
            Long userId = jwtUtil.getUserIdFromRequest(request);
            chatService.markAsRead(messageId, userId);
            
            Result<Void> result = Result.success();
            result.setMessage("操作成功");
            return result;
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }
    
    /**
     * 批量标记消息为已读
     */
    @ApiOperation("批量标记消息为已读")
    @PutMapping("/read/batch")
    public Result<Void> markBatchAsRead(@RequestBody List<Long> messageIds) {
        try {
            chatService.markBatchAsRead(messageIds);
            
            Result<Void> result = Result.success();
            result.setMessage("操作成功");
            return result;
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }
    
    /**
     * 标记与某人的所有未读消息为已读
     */
    @ApiOperation("标记与某人的所有未读消息为已读")
    @PutMapping("/read/all/{friendId}")
    public Result<Void> markAllAsReadFrom(@PathVariable Long friendId,
                                         HttpServletRequest request) {
        try {
            Long userId = jwtUtil.getUserIdFromRequest(request);
            chatService.markAllAsReadFrom(friendId, userId);
            
            Result<Void> result = Result.success();
            result.setMessage("操作成功");
            return result;
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }
    
    /**
     * 撤回消息
     */
    @ApiOperation("撤回消息")
    @PostMapping("/recall/{messageId}")
    public Result<Void> recallMessage(@PathVariable Long messageId,
                                      HttpServletRequest request) {
        try {
            Long userId = jwtUtil.getUserIdFromRequest(request);
            chatService.recallMessage(messageId, userId);
            
            Result<Void> result = Result.success();
            result.setMessage("消息已撤回");
            return result;
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }
    
    /**
     * 获取未读消息数
     */
    @ApiOperation("获取未读消息数")
    @GetMapping("/unread-count")
    public Result<Long> getUnreadCount(HttpServletRequest request) {
        try {
            Long userId = jwtUtil.getUserIdFromRequest(request);
            long count = chatService.getUnreadCount(userId);
            return Result.success("查询成功", count);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }
    
    /**
     * 获取来自某人的未读消息数
     */
    @ApiOperation("获取来自某人的未读消息数")
    @GetMapping("/unread-count/{friendId}")
    public Result<Long> getUnreadCountFrom(@PathVariable Long friendId,
                                           HttpServletRequest request) {
        try {
            Long userId = jwtUtil.getUserIdFromRequest(request);
            long count = chatService.getUnreadCountFrom(friendId, userId);
            return Result.success("查询成功", count);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }
}

