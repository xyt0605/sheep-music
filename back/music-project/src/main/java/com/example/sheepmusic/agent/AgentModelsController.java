package com.example.sheepmusic.agent;

import com.example.sheepmusic.common.Result;
import com.example.sheepmusic.utils.JwtUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestClient;

import java.net.URI;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 厂商实时模型列表（agent v1 P3 配置面板"获取模型列表"按钮）
 * 代理调用厂商的 OpenAI 兼容 GET {baseUrl}/models（多数兼容厂商支持；
 * 不支持的返回空列表 + 提示，前端引导手输模型名）
 */
@Slf4j
@Tag(name = "AI 连接配置")
@RestController
@RequestMapping("/agent/config")
@RequiredArgsConstructor
public class AgentModelsController {

    private final UserAiConfigService configService;
    private final JwtUtil jwtUtil;

    @Operation(summary = "拉取厂商实时模型列表")
    @GetMapping("/models")
    public Result<Map<String, Object>> models(@RequestParam String baseUrl,
                                              @RequestParam(required = false) String apiKey,
                                              HttpServletRequest request) {
        Long userId = jwtUtil.getUserIdFromRequest(request);
        if (baseUrl == null || baseUrl.isBlank() || !(baseUrl.startsWith("https://") || baseUrl.startsWith("http://"))) {
            return Result.error(400, "接口地址不合法");
        }
        // 未传 key 时用已保存配置里的密钥
        String key = apiKey;
        if (key == null || key.isBlank()) {
            UserAiConfigService.AiConfig cfg = configService.resolve(userId);
            key = cfg == null ? null : cfg.apiKey();
        }
        if (key == null || key.isBlank()) {
            return Result.error(400, "请先填写 API Key 或保存配置");
        }
        try {
            String url = (baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl) + "/models";
            org.springframework.http.client.SimpleClientHttpRequestFactory rf = new org.springframework.http.client.SimpleClientHttpRequestFactory();
            rf.setConnectTimeout(5000);
            rf.setReadTimeout(10000);
            String body = RestClient.builder()
                    .requestFactory(rf)
                    .build()
                    .get()
                    .uri(URI.create(url))
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + key)
                    .retrieve()
                    .body(String.class);
            com.fasterxml.jackson.databind.JsonNode root = new com.fasterxml.jackson.databind.ObjectMapper().readTree(body);
            List<String> ids = new ArrayList<>();
            if (root.path("data").isArray()) {
                root.path("data").forEach(n -> {
                    String id = n.path("id").asText("");
                    if (!id.isBlank()) {
                        ids.add(id);
                    }
                });
            }
            ids.sort(String.CASE_INSENSITIVE_ORDER);
            Map<String, Object> out = new LinkedHashMap<>();
            out.put("models", ids);
            out.put("supported", !ids.isEmpty());
            return Result.success(out);
        } catch (Exception e) {
            log.debug("拉取厂商模型列表失败 [{}]: {}", baseUrl, e.getMessage());
            Map<String, Object> out = new LinkedHashMap<>();
            out.put("models", List.of());
            out.put("supported", false);
            out.put("message", "该厂商不支持模型列表接口，请手动输入模型名");
            return Result.success(out);
        }
    }
}
