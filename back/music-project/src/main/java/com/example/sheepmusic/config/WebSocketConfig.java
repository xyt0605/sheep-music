package com.example.sheepmusic.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

/**
 * WebSocket STOMP 配置
 */
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        // 订阅前缀，客户端可订阅 /topic/** 或 /queue/**
        config.enableSimpleBroker("/topic", "/queue");
        // 应用前缀，客户端发送到 /app/** 由 @MessageMapping 处理
        config.setApplicationDestinationPrefixes("/app");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // 注册端点，支持SockJS，前端连接 /ws-chat
        registry.addEndpoint("/ws-chat")
                .setAllowedOriginPatterns("*")
                .withSockJS();

        // 兼容部分环境未去除 /api 前缀的代理配置
        registry.addEndpoint("/api/ws-chat")
                .setAllowedOriginPatterns("*")
                .withSockJS();
    }
}