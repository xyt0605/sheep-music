package com.example.sheepmusic.agent;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * 直连 OpenAI 兼容 /chat/completions 的流式客户端（JDK HttpClient）。
 *
 * <p>相比 Spring AI 的阻塞式 {@code call()}：
 * <ol>
 *   <li>流式读取，实时抽取 delta 里的 {@code reasoning_content}（推理模型的思考链），
 *       通过回调外发 → 前端可视化"模型正在想什么"，长等待不再是黑盒；</li>
 *   <li>支持 thinking 开关（智谱 GLM-4.5+ 的 {@code thinking.type=enabled/disabled}），
 *       关闭思考即贴近模型原生速度（同一模型在别处直调多快，这里就多快）。</li>
 * </ol>
 * 返回值 = 完整正文（{@code content} 累积），供编排器沿用既有 JSON 解析逻辑，零改动下游。
 */
@Slf4j
@Service
public class LlmStreamClient {

    private final ObjectMapper mapper = new ObjectMapper();
    private final HttpClient http = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(15))
            .build();

    /**
     * 流式调用，返回完整正文。
     *
     * @param onReasoning 思考内容增量回调（可为 null）；仅推理模型且思考开启时有值
     * @return 模型输出的完整正文（不含思考内容）
     */
    public String stream(UserAiConfigService.AiConfig cfg, String system, String user,
                         int maxTokens, double temperature, Consumer<String> onReasoning) {
        return stream(cfg, system, user, null, maxTokens, temperature, onReasoning);
    }

    /**
     * 多模态流式调用：imageDataUrls 非空时 user content 升级为 OpenAI 视觉格式数组
     * （text + image_url data URL），需要所选模型本身支持视觉（如 glm-4.5v / glm-4v-plus）
     */
    public String stream(UserAiConfigService.AiConfig cfg, String system, String user,
                         java.util.List<String> imageDataUrls,
                         int maxTokens, double temperature, Consumer<String> onReasoning) {
        try {
            String base = cfg.baseUrl();
            if (base.endsWith("/")) {
                base = base.substring(0, base.length() - 1);
            }
            String url = base + "/chat/completions";

            ObjectNode body = mapper.createObjectNode();
            body.put("model", cfg.model());
            body.put("stream", true);
            body.put("temperature", temperature);
            body.put("max_tokens", maxTokens);
            ArrayNode messages = body.putArray("messages");
            if (system != null && !system.isBlank()) {
                ObjectNode sys = messages.addObject();
                sys.put("role", "system");
                sys.put("content", system);
            }
            ObjectNode usr = messages.addObject();
            usr.put("role", "user");
            if (imageDataUrls != null && !imageDataUrls.isEmpty()) {
                ArrayNode content = mapper.createArrayNode();
                ObjectNode textPart = content.addObject();
                textPart.put("type", "text");
                textPart.put("text", user);
                for (String dataUrl : imageDataUrls) {
                    ObjectNode imgPart = content.addObject();
                    imgPart.put("type", "image_url");
                    imgPart.putObject("image_url").put("url", dataUrl);
                }
                usr.set("content", content);
            } else {
                usr.put("content", user);
            }

            // thinking 开关：目前仅智谱 GLM（open.bigmodel.cn）走 thinking.type 这一格式；
            // 其余厂商思考与否由所选模型决定（如 deepseek-chat 不思考、deepseek-reasoner 思考），
            // 贸然给它们塞 thinking 字段可能 400，故这里按 baseUrl 收敛作用域。
            String mode = cfg.thinkingMode();
            if (mode != null && !mode.isBlank() && !"auto".equals(mode) && base.contains("bigmodel")) {
                ObjectNode thinking = body.putObject("thinking");
                thinking.put("type", "disabled".equals(mode) ? "disabled" : "enabled");
            }

            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(180))
                    .header("Authorization", "Bearer " + cfg.apiKey())
                    .header("Content-Type", "application/json")
                    .header("Accept", "text/event-stream")
                    .POST(HttpRequest.BodyPublishers.ofString(mapper.writeValueAsString(body)))
                    .build();

            HttpResponse<Stream<String>> resp = http.send(req, HttpResponse.BodyHandlers.ofLines());
            if (resp.statusCode() / 100 != 2) {
                String err;
                try (Stream<String> s = resp.body()) {
                    err = s.collect(Collectors.joining("\n"));
                }
                throw new RuntimeException("HTTP " + resp.statusCode() + " " + truncate(err, 300));
            }

            StringBuilder content = new StringBuilder();
            try (Stream<String> lines = resp.body()) {
                lines.forEach(line -> appendDelta(line, content, onReasoning));
            }
            return content.toString();
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    /** 解析单行 SSE：抽取 delta.reasoning_content(思考) 与 delta.content(正文) */
    private void appendDelta(String line, StringBuilder content, Consumer<String> onReasoning) {
        if (line == null || !line.startsWith("data:")) {
            return;
        }
        String data = line.substring(5).trim();
        if (data.isEmpty() || "[DONE]".equals(data)) {
            return;
        }
        try {
            JsonNode delta = mapper.readTree(data).path("choices").path(0).path("delta");
            // 思考内容：智谱/DeepSeek 用 reasoning_content；部分聚合网关用 reasoning
            JsonNode rc = delta.get("reasoning_content");
            if (rc == null || rc.isNull()) {
                rc = delta.get("reasoning");
            }
            if (rc != null && !rc.isNull() && onReasoning != null) {
                String s = rc.asText();
                if (!s.isEmpty()) {
                    onReasoning.accept(s);
                }
            }
            JsonNode c = delta.get("content");
            if (c != null && !c.isNull()) {
                content.append(c.asText());
            }
        } catch (Exception parse) {
            log.trace("跳过无法解析的流式行: {}", parse.getMessage());
        }
    }

    private String truncate(String s, int n) {
        if (s == null) {
            return "";
        }
        return s.length() <= n ? s : s.substring(0, n);
    }
}
