package com.example.sheepmusic.agent;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 按用户配置动态构建 ChatClient（agent v1 P3 BYOK）
 * 缓存键 = 配置摘要（apiKey 摘要 + baseUrl + model），避免每轮重建；上限 50，溢出清空
 */
@Service
public class ChatClientFactory {

    public static final String DEFAULT_BASE_URL = "https://open.bigmodel.cn/api/paas/v4";
    public static final String DEFAULT_MODEL = "glm-4.5-flash";

    private final Map<String, ChatClient> cache = new ConcurrentHashMap<>();

    public ChatClient create(String apiKey, String baseUrl, String model) {
        String cacheKey = Integer.toHexString(apiKey.hashCode()) + "|" + baseUrl + "|" + model;
        if (cache.size() > 50) {
            cache.clear();
        }
        return cache.computeIfAbsent(cacheKey, k -> {
            // 智谱路径适配：base 已含 /v4，completions-path 为 /chat/completions
            OpenAiApi api = OpenAiApi.builder()
                    .baseUrl(baseUrl)
                    .apiKey(apiKey)
                    .completionsPath("/chat/completions")
                    .build();
            OpenAiChatModel chatModel = OpenAiChatModel.builder()
                    .openAiApi(api)
                    .defaultOptions(OpenAiChatOptions.builder().model(model).build())
                    .build();
            return ChatClient.builder(chatModel).build();
        });
    }
}
