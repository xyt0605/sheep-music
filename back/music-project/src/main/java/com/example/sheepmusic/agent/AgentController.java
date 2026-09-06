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
@Tag(name = "小屋 DJ")
@RestController
@RequestMapping("/agent")
@CrossOrigin
public class AgentController {

    @Autowired
    private AgentOrchestrator orchestrator;

    @Autowired
    private JwtUtil jwtUtil;

    private final ExecutorService executor = Executors.newFixedThreadPool(4);

    @Operation(summary = "小屋 DJ 对话（SSE 流式）")
    @PostMapping(value = "/dj/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter djStream(@RequestBody Map<String, String> body, HttpServletRequest request) {
        SseEmitter emitter = new SseEmitter(0L);
        Long userId = jwtUtil.getUserIdFromRequest(request);
        String query = body == null || body.get("query") == null ? "" : body.get("query").trim();

        if (query.isEmpty()) {
            sendAndComplete(emitter, "error", Map.of("message", "跟 DJ 说点想听什么吧～"));
            return emitter;
        }
        if (!orchestrator.isConfigured()) {
            log.warn("小屋 DJ 未配置密钥，降级返回提示");
            sendAndComplete(emitter, "error", Map.of("message",
                    "DJ 还没接上大模型：请配置环境变量 AGENT_API_KEY（智谱开放平台密钥）后重启服务"));
            return emitter;
        }
        executor.submit(() -> orchestrator.run(userId, query, emitter));
        return emitter;
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
