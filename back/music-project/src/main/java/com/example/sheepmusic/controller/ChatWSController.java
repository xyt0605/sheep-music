package com.example.sheepmusic.controller;

import com.example.sheepmusic.dto.ChatMessageRequest;
import com.example.sheepmusic.entity.ChatMessage;
import com.example.sheepmusic.service.ChatService;
import com.example.sheepmusic.utils.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Controller;

/**
 * WebSocket 聊天控制器（STOMP）
 */
@Controller
public class ChatWSController {

    @Autowired
    private ChatService chatService;

    @Autowired
    private JwtUtil jwtUtil;

    /**
     * 处理聊天消息发送
     * 前端发布到 /app/chat/send，需携带 Authorization Header（Bearer token）
     */
    @MessageMapping("/chat/send")
    public void send(@Payload ChatMessageRequest request,
                     @Header(name = "Authorization", required = false) String authorization) {
        Long userId = null;
        if (authorization != null && authorization.startsWith("Bearer ")) {
            String token = authorization.substring(7);
            userId = jwtUtil.getUserIdFromToken(token);
        }
        if (userId == null) {
            throw new RuntimeException("未认证的WebSocket消息");
        }
        // 调用业务服务保存并广播
        ChatMessage message = chatService.sendMessage(
                userId,
                request.getReceiverId(),
                request.getType(),
                request.getContent(),
                request.getExtra()
        );
        // ChatService 已负责广播，无需重复推送
    }
}