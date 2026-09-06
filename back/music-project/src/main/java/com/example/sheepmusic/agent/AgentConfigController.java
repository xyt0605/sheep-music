package com.example.sheepmusic.agent;

import com.example.sheepmusic.common.Result;
import com.example.sheepmusic.utils.JwtUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 用户 AI 连接配置（agent v1 P3 BYOK）：可视化配置板块的后端
 * 只操作当前登录用户自己的配置；API Key 加密落库、永不明文回传
 */
@Slf4j
@Tag(name = "AI 连接配置")
@RestController
@RequestMapping("/agent/config")
@CrossOrigin
@RequiredArgsConstructor
public class AgentConfigController {

    private final UserAiConfigService configService;
    private final JwtUtil jwtUtil;

    @Operation(summary = "查询配置状态（key 脱敏）")
    @GetMapping
    public Result<Map<String, Object>> status(HttpServletRequest request) {
        Long userId = jwtUtil.getUserIdFromRequest(request);
        return Result.success(configService.status(userId));
    }

    @Operation(summary = "保存配置（apiKey 必填，baseUrl/model 缺省用默认值）")
    @PostMapping
    public Result<Map<String, Object>> save(@RequestBody Map<String, String> body, HttpServletRequest request) {
        Long userId = jwtUtil.getUserIdFromRequest(request);
        try {
            configService.save(userId,
                    body == null ? null : body.get("apiKey"),
                    body == null ? null : body.get("baseUrl"),
                    body == null ? null : body.get("model"));
            return Result.success(configService.status(userId));
        } catch (IllegalArgumentException e) {
            return Result.error(400, e.getMessage());
        } catch (Exception e) {
            log.error("保存 AI 配置失败", e);
            return Result.error("保存失败，请稍后再试");
        }
    }

    @Operation(summary = "清除配置")
    @DeleteMapping
    public Result<Map<String, Object>> clear(HttpServletRequest request) {
        Long userId = jwtUtil.getUserIdFromRequest(request);
        configService.clear(userId);
        return Result.success(configService.status(userId));
    }

    @Operation(summary = "连接测试（apiKey 缺省时用已保存配置）")
    @PostMapping("/test")
    public Result<Map<String, Object>> test(@RequestBody(required = false) Map<String, String> body,
                                            HttpServletRequest request) {
        Long userId = jwtUtil.getUserIdFromRequest(request);
        Map<String, Object> out = configService.test(userId,
                body == null ? null : body.get("apiKey"),
                body == null ? null : body.get("baseUrl"),
                body == null ? null : body.get("model"));
        return Result.success(out);
    }
}
