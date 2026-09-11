package com.example.sheepmusic.agent;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * AI 连接配置服务（agent v1 P4：管理员统一配置）
 * 全局配置存 userId=0 的行；管理员保存/清除/测试均操作全局行；
 * resolve 优先全局行，无全局行时回退读 admin 账号自己的行（BYOK 时期遗留，无缝兼容）
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserAiConfigService {

    /** 全局配置行的 userId（普通用户 ID 从 1 自增，0 永远不会冲突） */
    public static final long GLOBAL_USER_ID = 0L;

    private final UserAiConfigRepository repo;
    private final AiCryptoUtil crypto;
    private final ChatClientFactory factory;
    private final com.example.sheepmusic.repository.UserRepository adminRepo;

    public record AiConfig(String apiKey, String baseUrl, String model, String thinkingMode) {
    }

    /** 解析有效配置：全局行优先 → 回退 admin 账号个人行；都没有返回 null */
    public AiConfig resolve(Long userId) {
        AiConfig cfg = readRow(GLOBAL_USER_ID);
        if (cfg == null) {
            cfg = readRow(resolveAdminUserId());
        }
        return cfg;
    }

    /** 全局配置状态（状态视图与 resolve() 同源：全局行 → 回退 admin 账号旧配置行） */
    public Map<String, Object> globalStatus() {
        UserAiConfig c = repo.findByUserId(GLOBAL_USER_ID);
        String source = "global";
        if (c == null) {
            Long adminId = resolveAdminUserId();
            if (adminId != null) {
                c = repo.findByUserId(adminId);
                source = "admin-legacy";
            }
        }
        Map<String, Object> out = new LinkedHashMap<>();
        boolean configured = c != null;
        out.put("configured", configured);
        if (configured) {
            out.put("baseUrl", c.getBaseUrl());
            out.put("model", c.getModel());
            out.put("thinkingMode", c.getThinkingMode() == null || c.getThinkingMode().isBlank() ? "auto" : c.getThinkingMode());
            out.put("apiKeyMasked", mask(c.getApiKeyEnc()));
        }
        out.put("source", source);
        out.put("defaultBaseUrl", ChatClientFactory.DEFAULT_BASE_URL);
        out.put("defaultModel", ChatClientFactory.DEFAULT_MODEL);
        return out;
    }

    /** 保存全局配置：apiKey 留空时沿用已有全局行；无全局行则迁移 admin 账号旧配置的密钥（一键接管 BYOK 遗产）；首次都没有才要求必填 */
    public void saveGlobal(String apiKey, String baseUrl, String model, String thinkingMode) {
        UserAiConfig c = repo.findByUserId(GLOBAL_USER_ID);
        boolean hasKey = apiKey != null && !apiKey.isBlank();
        if (!hasKey && c == null) {
            Long legacyId = resolveAdminUserId();
            UserAiConfig legacy = legacyId == null ? null : repo.findByUserId(legacyId);
            if (legacy != null) {
                // 迁移旧密钥：管理员只需在面板里改模型/地址，不必重贴 Key
                c = new UserAiConfig();
                c.setUserId(GLOBAL_USER_ID);
                c.setApiKeyEnc(legacy.getApiKeyEnc());
            } else {
                throw new IllegalArgumentException("API Key 不能为空");
            }
        }
        if (c == null) {
            c = new UserAiConfig();
            c.setUserId(GLOBAL_USER_ID);
        }
        if (hasKey) {
            c.setApiKeyEnc(crypto.encrypt(apiKey.trim()));
        }
        c.setBaseUrl(baseUrl == null || baseUrl.isBlank() ? ChatClientFactory.DEFAULT_BASE_URL : baseUrl.trim());
        c.setModel(model == null || model.isBlank() ? ChatClientFactory.DEFAULT_MODEL : model.trim());
        c.setThinkingMode(normalizeThinkingMode(thinkingMode));
        c.setUpdateTime(LocalDateTime.now());
        repo.save(c);
    }

    @Transactional
    public void clearGlobal() {
        repo.deleteByUserId(GLOBAL_USER_ID);
    }

    /** admin 账号的 userId（BYOK 时期管理员可能配在自己名下）；查不到返回 null */
    private Long resolveAdminUserId() {
        return adminRepo.findFirstByRoleOrderByIdAsc("admin").map(com.example.sheepmusic.entity.User::getId).orElse(null);
    }

    private AiConfig readRow(Long userId) {
        if (userId == null) {
            return null;
        }
        UserAiConfig c = repo.findByUserId(userId);
        if (c == null) {
            return null;
        }
        try {
            String mode = c.getThinkingMode() == null || c.getThinkingMode().isBlank() ? "auto" : c.getThinkingMode();
            return new AiConfig(crypto.decrypt(c.getApiKeyEnc()), c.getBaseUrl(), c.getModel(), mode);
        } catch (Exception e) {
            log.error("AI 配置解密失败 userId={}", userId, e);
            return null;
        }
    }

    private String normalizeThinkingMode(String mode) {
        if (mode == null) {
            return "auto";
        }
        String m = mode.trim().toLowerCase();
        return switch (m) {
            case "enabled", "disabled" -> m;
            default -> "auto";
        };
    }

    private Map<String, Object> statusOf(Long userId) {
        UserAiConfig c = userId == null ? null : repo.findByUserId(userId);
        Map<String, Object> out = new LinkedHashMap<>();
        boolean configured = c != null;
        out.put("configured", configured);
        if (configured) {
            out.put("baseUrl", c.getBaseUrl());
            out.put("model", c.getModel());
            out.put("thinkingMode", c.getThinkingMode() == null || c.getThinkingMode().isBlank() ? "auto" : c.getThinkingMode());
            out.put("apiKeyMasked", mask(c.getApiKeyEnc()));
        }
        out.put("defaultBaseUrl", ChatClientFactory.DEFAULT_BASE_URL);
        out.put("defaultModel", ChatClientFactory.DEFAULT_MODEL);
        return out;
    }

    /** 连接测试：真实最小调用（推理模型会先思考，maxTokens 给足） */
    public Map<String, Object> test(String apiKey, String baseUrl, String model) {
        String key = apiKey == null || apiKey.isBlank() ? optionalGlobalKey() : apiKey.trim();
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

    /** 全局配置里的密钥（供"测试连接"未填 key 时使用） */
    public String optionalGlobalKey() {
        AiConfig cfg = readRow(GLOBAL_USER_ID);
        return cfg == null ? null : cfg.apiKey();
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
