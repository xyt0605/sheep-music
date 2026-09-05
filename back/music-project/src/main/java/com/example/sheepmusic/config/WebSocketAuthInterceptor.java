package com.example.sheepmusic.config;

import com.example.sheepmusic.utils.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;

import java.security.Principal;
import java.util.Collections;

/**
 * STOMP 入站拦截器：
 * 1. CONNECT 时校验 JWT（Authorization: Bearer xxx），未认证直接拒绝连接；
 * 2. SUBSCRIBE 时校验只能订阅自己的频道 /topic/user.{自己id}.*，
 *    防止任意用户窃听他人聊天/通知推送。
 */
@Component
public class WebSocketAuthInterceptor implements ChannelInterceptor {

    @Autowired
    private JwtUtil jwtUtil;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor =
            MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        if (accessor == null || accessor.getCommand() == null) {
            return message;
        }

        if (StompCommand.CONNECT.equals(accessor.getCommand())) {
            Long userId = authenticate(accessor);
            if (userId == null) {
                throw new MessagingException("未认证的WebSocket连接");
            }
            // 以 userId 作为 principal，后续 SUBSCRIBE 校验基于它
            UsernamePasswordAuthenticationToken user =
                new UsernamePasswordAuthenticationToken(
                    String.valueOf(userId), null,
                    Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER")));
            accessor.setUser(user);
        } else if (StompCommand.SUBSCRIBE.equals(accessor.getCommand())) {
            Principal principal = accessor.getUser();
            String destination = accessor.getDestination();
            String ownPrefix = "/topic/user." + (principal == null ? null : principal.getName()) + ".";
            if (principal == null || destination == null || !destination.startsWith(ownPrefix)) {
                throw new MessagingException("无权订阅该频道");
            }
        }

        return message;
    }

    /**
     * 从 CONNECT 帧的 native header 中解析并校验 JWT，返回用户ID
     */
    private Long authenticate(StompHeaderAccessor accessor) {
        String authorization = accessor.getFirstNativeHeader("Authorization");
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            return null;
        }
        String token = authorization.substring(7);
        if (!jwtUtil.validateToken(token)) {
            return null;
        }
        return jwtUtil.getUserIdFromToken(token);
    }
}
