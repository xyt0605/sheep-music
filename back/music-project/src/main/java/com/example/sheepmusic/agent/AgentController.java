package com.example.sheepmusic.agent;

import com.example.sheepmusic.utils.JwtUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 小屋 DJ（agent v1，docs/specs/agent-v1/）
 * SSE 事件流：stage / thought / action / observation / text_delta / song_card / error / done
 * 未配 AGENT_API_KEY 时以 error 事件优雅降级；密钥不入仓库
 */
@Slf4j
@Tag(name = "奶包")
@RestController
@RequestMapping("/agent")
@CrossOrigin
public class AgentController {

    @Autowired
    private AgentOrchestrator orchestrator;

    @Autowired
    private UserAiConfigService configService;

    @Autowired
    private JwtUtil jwtUtil;

    private final ExecutorService executor = Executors.newFixedThreadPool(4);

    @Operation(summary = "奶包对话（SSE 流式，支持会话记忆与图片多模态）")
    @PostMapping(value = "/dj/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter djStream(@RequestBody Map<String, Object> body, HttpServletRequest request) {
        SseEmitter emitter = new SseEmitter(0L);
        Long userId = jwtUtil.getUserIdFromRequest(request);
        String query = body == null || body.get("query") == null ? "" : String.valueOf(body.get("query")).trim();
        String sessionId = body == null || body.get("sessionId") == null ? "" : String.valueOf(body.get("sessionId")).trim();
        java.util.List<String> images = extractImages(body);

        if (query.isEmpty() && images.isEmpty()) {
            sendAndComplete(emitter, "error", Map.of("message", "跟奶包说点想听什么，或附一张图片吧～"));
            return emitter;
        }
        // P4：管理员统一配置；无配置 → 明确指引到系统设置
        if (configService.resolve(userId) == null) {
            log.debug("用户 {} 未配置 AI 密钥，DJ 降级提示", userId);
            sendAndComplete(emitter, "error", Map.of("message",
                    "管理员还没有配置 AI 连接，奶包暂时不能营业。请在 管理后台 → 系统设置 完成 AI 连接配置"));
            return emitter;
        }
        String sid = sessionId.isBlank() ? java.util.UUID.randomUUID().toString() : sessionId;
        executor.submit(() -> orchestrator.run(userId, sid, query, images, emitter));
        return emitter;
    }

    /** 附图校验：≤3 张、data URL 格式、单张 ≤1.5M 字符（前端已压缩）；不合规直接忽略 */
    private java.util.List<String> extractImages(Map<String, Object> body) {
        Object raw = body == null ? null : body.get("images");
        if (!(raw instanceof java.util.List<?> list)) {
            return java.util.List.of();
        }
        java.util.List<String> out = new java.util.ArrayList<>();
        for (Object o : list) {
            if (!(o instanceof String s) || s.isBlank() || !s.startsWith("data:image/")
                    || s.length() > 1_500_000) {
                continue;
            }
            out.add(s);
            if (out.size() >= 3) {
                break;
            }
        }
        return out;
    }

    private void sendAndComplete(SseEmitter emitter, String event, Object data) {
        try {
            emitter.send(SseEmitter.event().name(event).data(data));
            emitter.complete();
        } catch (Exception e) {
            log.debug("SSE 降级推送失败: {}", e.getMessage());
        }
    }
}
