package com.example.sheepmusic.config;

import com.example.sheepmusic.security.JwtAuthenticationFilter;
import jakarta.servlet.DispatcherType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Spring Security 配置（Security 6：SecurityFilterChain + lambda DSL）
 *
 * 规则与旧版 WebSecurityConfigurerAdapter 逐条对齐。
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Autowired
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @Autowired
    private RestAuthenticationEntryPoint restAuthenticationEntryPoint;

    /*密码加密器 Bean*/
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // 禁用CSRF（因为使用JWT）
                .csrf(csrf -> csrf.disable())
                // 允许跨域（使用 MvcCorsConfigurer/CorsConfig 提供的配置）
                .cors(cors -> {})
                // 配置Session管理为无状态
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                // 配置请求权限
                .authorizeHttpRequests(auth -> auth
                        // Security 6.1 起错误页/转发电会被鉴权，匿名访问 /error 会用 401
                        // 覆盖真实状态码（403/404 全变 401），这里恢复 Boot 2 的语义
                        .dispatcherTypeMatchers(DispatcherType.ERROR, DispatcherType.FORWARD).permitAll()
                        // 放行 OPTIONS 请求（CORS 预检）
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        // 放行登录注册接口（同时支持 /auth/** 和 /api/auth/**）
                        .requestMatchers("/auth/**", "/api/auth/**").permitAll()
                        // 放行接口文档（knife4j 4.x / springdoc）
                        .requestMatchers("/doc.html", "/webjars/**", "/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()
                        // 放行静态资源
                        .requestMatchers("/static/**", "/").permitAll()
                        // 放行WebSocket端点
                        .requestMatchers("/ws/**", "/ws-chat/**", "/api/ws/**", "/api/ws-chat/**").permitAll()
                        // 放行热门搜索接口（公开接口）
                        .requestMatchers(HttpMethod.GET, "/api/user/search-history/hot").permitAll()
                        // 放行外源音频流代理（<audio> 无法携带 JWT；曲库供应链 v1，暴露面由数字 trackId+白名单收敛）
                        .requestMatchers("/music/external/stream", "/api/music/external/stream").permitAll()
                        // 管理员接口（只有管理员可访问）
                        .requestMatchers("/admin/**", "/api/admin/**").hasRole("ADMIN")
                        // 其他请求需要认证
                        .anyRequest().authenticated()
                )
                // 未认证时返回401（区别于已认证但无权限的403），便于前端跳转登录
                .exceptionHandling(ex -> ex.authenticationEntryPoint(restAuthenticationEntryPoint))
                // 添加JWT过滤器
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }
}
