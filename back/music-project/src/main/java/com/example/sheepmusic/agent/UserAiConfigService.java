package com.example.sheepmusic.agent;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 用户 AI 配置服务（agent v1 P3 BYOK）：存取/脱敏/清除/连接测试
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserAiConfigService {

    private final UserAiConfigRepository repo;
    private final AiCryptoUtil crypto;
    private final ChatClientFactory factory;

    public record AiConfig(String apiKey, String baseUrl, String model) {
    }

    /** 解析用户配置；未配置或解密失败返回 null */
    public AiConfig resolve(Long userId) {
        UserAiConfig c = repo.findByUserId(userId);
        if (c == null) {
            return null;
        }
        try {
            return new AiConfig(crypto.decrypt(c.getApiKeyEnc()), c.getBaseUrl(), c.getModel());
        } catch (Exception e) {
            log.error("用户 AI 配置解密失败 userId={}", userId, e);
            return null;
        }
    }

    public Map<String, Object> status(Long userId) {
        UserAiConfig c = repo.findByUserId(userId);
        Map<String, Object> out = new LinkedHashMap<>();
        boolean configured = c != null;
        out.put("configured", configured);
        if (configured) {
            out.put("baseUrl", c.getBaseUrl());
            out.put("model", c.getModel());
            out.put("apiKeyMasked", mask(c.getApiKeyEnc()));
        }
        out.put("defaultBaseUrl", ChatClientFactory.DEFAULT_BASE_URL);
        out.put("defaultModel", ChatClientFactory.DEFAULT_MODEL);
        return out;
    }

    public void save(Long userId, String apiKey, String baseUrl, String model) {
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalArgumentException("API Key 不能为空");
        }
        UserAiConfig c = repo.findByUserId(userId);
        if (c == null) {
            c = new UserAiConfig();
            c.setUserId(userId);
        }
        c.setApiKeyEnc(crypto.encrypt(apiKey.trim()));
        c.setBaseUrl(baseUrl == null || baseUrl.isBlank() ? ChatClientFactory.DEFAULT_BASE_URL : baseUrl.trim());
        c.setModel(model == null || model.isBlank() ? ChatClientFactory.DEFAULT_MODEL : model.trim());
        c.setUpdateTime(LocalDateTime.now());
        repo.save(c);
    }

    @Transactional
    public void clear(Long userId) {
        repo.deleteByUserId(userId);
    }

    /** 连接测试：真实最小调用（推理模型会先思考，maxTokens 给足） */
    public Map<String, Object> test(Long userId, String apiKey, String baseUrl, String model) {
        String key = apiKey == null || apiKey.isBlank() ? optionalSavedKey(userId) : apiKey.trim();
        if (key == null || key.isBlank()) {
            return Map.of("ok", false, "message", "请先填写 API Key");
        }
        String url = baseUrl == null || baseUrl.isBlank() ? ChatClientFactory.DEFAULT_BASE_URL : baseUrl.trim();
        String mdl = model == null || model.isBlank() ? ChatClientFactory.DEFAULT_MODEL : model.trim();
        long t0 = System.currentTimeMillis();
        try {
            factory.create(key, url, mdl).prompt()
                    .user("回复两个字：收到")
                    .options(org.springframework.ai.openai.OpenAiChatOptions.builder().maxTokens(512).temperature(0.1).build())
                    .call()
                    .content();
            Map<String, Object> out = new LinkedHashMap<>();
            out.put("ok", true);
            out.put("message", "连接成功（" + mdl + "）");
            out.put("latencyMs", System.currentTimeMillis() - t0);
            return out;
        } catch (Exception e) {
            Map<String, Object> out = new LinkedHashMap<>();
            out.put("ok", false);
            out.put("message", "连接失败：" + String.valueOf(e.getMessage()).substring(0, Math.min(160, String.valueOf(e.getMessage()).length())));
            out.put("latencyMs", System.currentTimeMillis() - t0);
            return out;
        }
    }

    private String optionalSavedKey(Long userId) {
        UserAiConfig c = repo.findByUserId(userId);
        try {
            return c == null ? null : crypto.decrypt(c.getApiKeyEnc());
        } catch (Exception e) {
            return null;
        }
    }

    /** 脱敏：前 4 后 4 */
    private String mask(String apiKeyEnc) {
        try {
            String plain = crypto.decrypt(apiKeyEnc);
            if (plain.length() <= 8) {
                return "****";
            }
            return plain.substring(0, 4) + "****" + plain.substring(plain.length() - 4);
        } catch (Exception e) {
            return "****";
        }
    }
}
