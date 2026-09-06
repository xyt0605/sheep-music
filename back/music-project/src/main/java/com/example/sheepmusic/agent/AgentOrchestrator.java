package com.example.sheepmusic.agent;

import com.example.sheepmusic.entity.Song;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 小屋 DJ 编排器（agent v1）：Dispatcher → Librarian(ReAct) → DJ → Critic(Reflection)
 * 全过程以 SSE 事件推播（stage/thought/action/observation/text_delta/song_card/error/done）
 * 防幻觉：候选以 ref 下标提名，卡片字段全部由编排器从工具返回的数据回填
 */
@Slf4j
@Service
public class AgentOrchestrator {

    private static final int MAX_LIBRARIAN_STEPS = 4;
    private static final int MAX_ROUNDS = 2;
    private static final int MAX_CANDIDATES = 10;

    private final DjTools tools;
    private final DjSessionStore sessionStore;
    private final UserAiConfigService configService;
    private final ChatClientFactory clientFactory;
    private final ObjectMapper mapper = new ObjectMapper();
    private final ExecutorService executor = Executors.newFixedThreadPool(4);

    public AgentOrchestrator(@Autowired DjTools tools,
                             @Autowired DjSessionStore sessionStore,
                             @Autowired UserAiConfigService configService,
                             @Autowired ChatClientFactory clientFactory) {
        this.tools = tools;
        this.sessionStore = sessionStore;
        this.configService = configService;
        this.clientFactory = clientFactory;
    }

    /** 在工作线程中执行整轮编排（由 Controller 提交到线程池） */
    public void run(Long userId, String sessionId, String query, SseEmitter emitter) {
        try {
            doRun(userId, sessionId, query, emitter);
            emitter.complete();
        } catch (org.springframework.web.util.NestedServletException | IllegalStateException e) {
            log.warn("DJ 会话中断: {}", e.getMessage());
            try {
                emitter.complete();
            } catch (Exception ignored) {
            }
        } catch (Exception e) {
            log.error("DJ 编排失败", e);
            sendSafe(emitter, "error", Map.of("message", friendly(e)));
            try {
                emitter.complete();
            } catch (Exception ignored) {
            }
        }
    }

    private void doRun(Long userId, String sessionId, String query, SseEmitter emitter) throws Exception {
        // ---------- BYOK：解析用户自己的 AI 配置（无配置 → 明确提示） ----------
        UserAiConfigService.AiConfig cfg = configService.resolve(userId);
        if (cfg == null) {
            send(emitter, "error", Map.of("message", "当前用户还没有配置密钥，无法使用。请点击抽屉右上角 ⚙ 完成 AI 连接配置"));
            return;
        }
        ChatClient chatClient = clientFactory.create(cfg.apiKey(), cfg.baseUrl(), cfg.model());

        // ---------- 会话记忆（P3）：MySQL 持久化，session 事件回传 + 历史上下文 ----------
        DjSessionStore.Context ctx = sessionStore.load(userId, sessionId);
        send(emitter, "session", Map.of("sessionId", sessionId));
        StringBuilder historyText = new StringBuilder();
        if (!ctx.turns().isEmpty()) {
            historyText.append("对话历史（最近在后）：\n");
            ctx.turns().stream()
                    .skip(Math.max(0, ctx.turns().size() - 6))
                    .forEach(t -> historyText.append(t.role().equals("user") ? "用户: " : "DJ: ").append(t.text()).append('\n'));
        }
        if (!ctx.lastTitles().isEmpty()) {
            historyText.append("上一轮已推荐（除非用户点名，避免重复）：").append(String.join("、", ctx.lastTitles())).append('\n');
        }
        Map<String, Object> taste = tools.tasteProfile(userId);
        if (!taste.isEmpty()) {
            historyText.append("听众口味画像：").append(toJson(taste)).append('\n');
        }

        // ---------- ① Dispatcher：意图拆解 ----------
        send(emitter, "stage", Map.of("stage", "dispatcher", "status", "start"));
        String intentJson = callLlm(chatClient, AgentPrompts.DISPATCHER, historyText + "用户需求：" + query, 1024, 0.3);
        Map<String, Object> intent = parseJson(intentJson);
        int wantCount = intVal(intent, "count", 8);
        send(emitter, "stage", Map.of("stage", "dispatcher", "status", "done", "intent", intent));

        // ---------- ② Librarian ReAct（Critic 不过时回炉，最多 2 轮） ----------
        List<Map<String, Object>> index = new ArrayList<>();   // ref → 条目
        Set<String> seen = new HashSet<>();
        List<Map<String, Object>> picked = null;
        String criticFeedback = null;

        for (int round = 1; round <= MAX_ROUNDS && picked == null; round++) {
            send(emitter, "stage", Map.of("stage", "librarian", "status", "start", "round", round));
            String history = round > 1
                    ? "上一轮质检未通过，改进意见：" + criticFeedback + "\n"
                    : "";
            String thought = null;

            for (int step = 1; step <= MAX_LIBRARIAN_STEPS && picked == null; step++) {
                String userMsg = historyText
                        + "用户需求原文：" + query
                        + "\n意图：" + intentJson
                        + "\n\n已收集条目：\n" + describe(index)
                        + "\n\n动作历史：\n" + (history.isEmpty() ? "（无）" : history)
                        + "\n请输出下一步 JSON。";
                Map<String, Object> act = null;
                for (int attempt = 0; attempt < 2 && act == null; attempt++) {
                    String extra = attempt == 0 ? "" : "\n（你上一次的输出无法解析，请只输出一个 JSON 对象。）";
                    try {
                        act = parseJson(callLlm(chatClient, AgentPrompts.LIBRARIAN, userMsg + extra, 1600, 0.4));
                    } catch (Exception e) {
                        log.debug("Librarian 第{}步第{}次输出解析失败: {}", step, attempt + 1, e.getMessage());
                    }
                }
                if (act == null) {
                    history += "步骤" + step + ": 输出解析失败，跳过\n";
                    continue;
                }
                thought = str(act, "thought", "");
                String action = str(act, "action", "");
                @SuppressWarnings("unchecked")
                Map<String, Object> args = act.get("args") instanceof Map ? (Map<String, Object>) act.get("args") : Map.of();
                send(emitter, "thought", Map.of("step", step, "round", round, "text", thought));
                send(emitter, "action", Map.of("step", step, "action", action, "args", args));

                String observation;
                switch (action) {
                    case "search_local" -> {
                        List<Map<String, Object>> items = tools.searchLocal(
                                str(args, "keyword", str(intent, "intentSummary", query)), intVal(args, "limit", 6));
                        observation = collect(items, index, seen);
                    }
                    case "search_web" -> {
                        List<Map<String, Object>> items = tools.searchWeb(
                                str(args, "keyword", fallbackKeyword(intent, query)), intVal(args, "limit", 6));
                        observation = collect(items, index, seen);
                    }
                    case "recommend" -> observation = collect(
                            tools.recommend(userId, intVal(args, "limit", 6)), index, seen);
                    case "finish" -> {
                        @SuppressWarnings("unchecked")
                        List<Number> refs = act.get("refs") instanceof List ? (List<Number>) act.get("refs") : List.of();
                        picked = pickByRefs(refs, index);
                        observation = "已提名 " + picked.size() + " 首";
                    }
                    default -> observation = "未知动作 " + action + "，可用：search_local/search_web/recommend/finish";
                }
                history += "步骤" + step + " " + action + " " + args + "\n观察: " + observation + "\n";
                send(emitter, "observation", Map.of("step", step, "text", observation));
            }

            if (picked == null) {
                picked = firstOf(index);   // 动作耗尽仍未 finish：用已收集条目兜底
            }

            // ---------- ④ Critic：Reflection ----------
            send(emitter, "stage", Map.of("stage", "critic", "status", "start", "round", round));
            Map<String, Object> verdict = null;
            try {
                verdict = parseJson(callLlm(chatClient, AgentPrompts.CRITIC,
                        "用户需求：" + query + "\n候选组：\n" + describe(picked), 800, 0.2));
            } catch (Exception e) {
                log.debug("Critic 输出解析失败，视为通过: {}", e.getMessage());
            }
            int score = verdict == null ? 80 : intVal(verdict, "score", 80);
            boolean pass = verdict != null && Boolean.TRUE.equals(verdict.get("pass")) || score >= 70;
            String feedback = verdict == null ? "" : str(verdict, "feedback", "");
            send(emitter, "stage", Map.of("stage", "critic", "status", "done", "round", round,
                    "score", score, "pass", pass, "feedback", feedback));
            if (pass) {
                break;
            }
            criticFeedback = feedback;
            picked = null;   // 回炉
        }

        if (picked == null || picked.isEmpty()) {
            picked = firstOf(index);
        }
        if (picked.isEmpty()) {
            send(emitter, "error", Map.of("message", "没找到合适的歌曲，换个说法试试？"));
            return;
        }

        // ---------- ③ DJ：串场词 + 逐首理由（P2：单次 JSON 调用，intro 切片伪流式） ----------
        send(emitter, "stage", Map.of("stage", "dj", "status", "start"));
        String djRaw = callLlm(chatClient, AgentPrompts.DJ,
                historyText + "用户需求：" + query + "\n候选歌曲（按推荐顺序）：\n" + describe(picked),
                1800, 0.85);
        String intro = "";
        List<String> reasons = List.of();
        try {
            Map<String, Object> djOut = parseJson(djRaw);
            intro = str(djOut, "intro", "");
            if (djOut.get("reasons") instanceof List<?> list) {
                reasons = list.stream().map(String::valueOf).toList();
            }
        } catch (Exception e) {
            log.debug("DJ 输出解析失败，退化为纯文本串场词: {}", e.getMessage());
            intro = djRaw == null ? "" : djRaw.trim();
        }
        if (intro.isBlank()) {
            intro = "这组歌为你挑好了，直接开听吧。";
        }
        for (int i = 0; i < intro.length(); i += 12) {
            send(emitter, "text_delta", Map.of("delta", intro.substring(i, Math.min(i + 12, intro.length()))));
        }
        send(emitter, "stage", Map.of("stage", "dj", "status", "done"));

        // ---------- 末端：song_card（字段全部来自工具数据，reason 来自 DJ） ----------
        List<Map<String, Object>> cards = new ArrayList<>();
        for (int i = 0; i < picked.size(); i++) {
            Map<String, Object> item = picked.get(i);
            Map<String, Object> card = new LinkedHashMap<>();
            boolean external = "gequhai".equals(item.get("type"));
            if (external) {
                card.put("source", "gequhai");
                card.put("sourceTrackId", item.get("sourceTrackId"));
                card.put("streamUrl", item.get("streamUrl"));
                card.put("isExternal", true);
            } else {
                card.put("songId", item.get("songId"));
                card.put("source", "local");
            }
            card.put("title", item.get("title"));
            card.put("artist", item.get("artist"));
            card.put("cover", item.get("cover"));
            card.put("reason", i < reasons.size() && !reasons.get(i).isBlank() ? reasons.get(i) : item.get("reason"));
            send(emitter, "song_card", card);
            cards.add(card);
        }

        // ---------- 会话记忆（P2）：写入本轮对话与已推荐清单 ----------
        sessionStore.appendTurn(userId, sessionId, "user", query, null);
        sessionStore.appendTurn(userId, sessionId, "dj", intro,
                picked.stream().map(i2 -> String.valueOf(i2.get("title"))).toList());
        send(emitter, "done", Map.of("count", cards.size(), "intro", intro));
    }

    // ========== 工具结果收集与提名 ==========

    /** 把工具条目登记进候选索引（去重、分配 ref），返回给 LLM 的观察摘要 */
    private String collect(List<Map<String, Object>> items, List<Map<String, Object>> index, Set<String> seen) {
        if (items == null || items.isEmpty()) {
            return "没有找到结果";
        }
        StringBuilder sb = new StringBuilder();
        for (Map<String, Object> item : items) {
            String key = (item.get("type") + "|" + item.get("title") + "|" + item.get("artist")).toLowerCase();
            if (!seen.add(key) || index.size() >= 40) {
                continue;
            }
            int ref = index.size();
            item.put("ref", ref);
            index.add(item);
            sb.append("[ref=").append(ref).append("] ")
                    .append(item.get("title")).append(" - ").append(item.get("artist"))
                    .append("（来源: ").append(item.get("type")).append("）\n");
        }
        return sb.isEmpty() ? "没有新结果（均为重复条目）" : sb.toString();
    }

    private List<Map<String, Object>> pickByRefs(List<Number> refs, List<Map<String, Object>> index) {
        List<Map<String, Object>> out = new ArrayList<>();
        Set<Integer> used = new HashSet<>();
        if (refs != null) {
            for (Number n : refs) {
                int ref = n.intValue();
                if (ref >= 0 && ref < index.size() && used.add(ref)) {
                    out.add(index.get(ref));
                    if (out.size() >= MAX_CANDIDATES) {
                        break;
                    }
                }
            }
        }
        return out;
    }

    /** 兜底：从已收集条目按序取前 N（Librarian 未 finish 时） */
    private List<Map<String, Object>> firstOf(List<Map<String, Object>> index) {
        List<Map<String, Object>> out = new ArrayList<>();
        for (Map<String, Object> item : index) {
            out.add(item);
            if (out.size() >= MAX_CANDIDATES) {
                break;
            }
        }
        return out;
    }

    private String describe(List<Map<String, Object>> items) {
        if (items.isEmpty()) {
            return "（暂无）";
        }
        StringBuilder sb = new StringBuilder();
        for (Map<String, Object> item : items) {
            Object ref = item.containsKey("ref") ? "[ref=" + item.get("ref") + "] " : "";
            sb.append(ref).append(item.get("title")).append(" - ").append(item.get("artist"))
                    .append("（来源: ").append(item.get("type")).append("）");
            if (item.get("reason") != null) {
                sb.append(" 推荐理由: ").append(item.get("reason"));
            }
            sb.append('\n');
        }
        return sb.toString();
    }

    private String fallbackKeyword(Map<String, Object> intent, String query) {
        StringBuilder kw = new StringBuilder();
        for (String k : List.of("artists", "genres")) {
            if (intent.get(k) instanceof List<?> list) {
                for (Object o : list) {
                    if (kw.length() > 0) {
                        kw.append(' ');
                    }
                    kw.append(o);
                }
            }
        }
        String mood = str(intent, "mood", "");
        if (!mood.isBlank()) {
            kw.append(' ').append(mood);
        }
        return kw.length() == 0 ? query : kw.toString();
    }

    // ========== LLM 调用与 JSON 解析 ==========

    private String callLlm(ChatClient chatClient, String system, String user, int maxTokens, double temperature) {
        return chatClient.prompt()
                .system(system)
                .user(user)
                .options(OpenAiChatOptions.builder().maxTokens(maxTokens).temperature(temperature).build())
                .call()
                .content();
    }

    /** 防御性 JSON 提取：剥代码围栏 + 截取首个平衡大括号块 */
    private Map<String, Object> parseJson(String text) throws Exception {
        if (text == null || text.isBlank()) {
            throw new IllegalStateException("空响应");
        }
        String t = text.trim();
        int fence = t.indexOf("```");
        if (fence >= 0) {
            int fenceEnd = t.indexOf("```", fence + 3);
            t = fenceEnd > fence ? t.substring(fence + 3, fenceEnd) : t.substring(fence + 3);
            t = t.replaceFirst("^json", "").trim();
        }
        int start = t.indexOf('{');
        if (start < 0) {
            throw new IllegalStateException("响应中无 JSON 对象");
        }
        int depth = 0;
        boolean inString = false;
        for (int i = start; i < t.length(); i++) {
            char c = t.charAt(i);
            if (inString) {
                if (c == '\\') {
                    i++;
                } else if (c == '"') {
                    inString = false;
                }
            } else if (c == '"') {
                inString = true;
            } else if (c == '{') {
                depth++;
            } else if (c == '}' && --depth == 0) {
                return mapper.readValue(t.substring(start, i + 1), new TypeReference<Map<String, Object>>() {
                });
            }
        }
        throw new IllegalStateException("JSON 未闭合");
    }

    private String str(Map<String, Object> m, String key, String def) {
        Object v = m == null ? null : m.get(key);
        return v == null || String.valueOf(v).isBlank() ? def : String.valueOf(v);
    }

    private int intVal(Map<String, Object> m, String key, int def) {
        Object v = m == null ? null : m.get(key);
        if (v instanceof Number n) {
            return n.intValue();
        }
        try {
            return v == null ? def : (int) Double.parseDouble(String.valueOf(v));
        } catch (Exception e) {
            return def;
        }
    }

    // ========== SSE 推送 ==========

    private void send(SseEmitter emitter, String event, Object data) {
        try {
            emitter.send(SseEmitter.event().name(event).data(toJson(data)));
        } catch (Exception e) {
            throw new IllegalStateException("SSE 推送失败（客户端可能已断开）", e);
        }
    }

    private void sendSafe(SseEmitter emitter, String event, Object data) {
        try {
            send(emitter, event, data);
        } catch (Exception ignored) {
        }
    }

    private String toJson(Object data) {
        try {
            return mapper.writeValueAsString(data);
        } catch (Exception e) {
            return "{}";
        }
    }

    private String friendly(Exception e) {
        String msg = String.valueOf(e.getMessage());
        if (msg.contains("401") || msg.toLowerCase().contains("api key") || msg.toLowerCase().contains("unauthorized")) {
            return "DJ 的大模型密钥无效，请检查 AGENT_API_KEY 配置";
        }
        if (msg.contains("429") || msg.toLowerCase().contains("rate")) {
            return "请求太频繁了，喝口水稍后再试";
        }
        return "DJ 开小差了，稍后再试～";
    }
}
