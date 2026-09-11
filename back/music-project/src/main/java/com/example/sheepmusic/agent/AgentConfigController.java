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
 * AI 连接配置（agent v1 P4：管理员统一配置）：后端
 * 全局配置对全员生效（resolve 见 UserAiConfigService）；
 * 读取任何登录用户可用（脱敏），写入/清除/测试仅管理员
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

    private boolean isAdmin(HttpServletRequest request) {
        String role = jwtUtil.getRoleFromRequest(request);
        return "admin".equalsIgnoreCase(role);
    }

    @Operation(summary = "查询全局配置状态（key 脱敏）")
    @GetMapping
    public Result<Map<String, Object>> status(HttpServletRequest request) {
        return Result.success(configService.globalStatus());
    }

    @Operation(summary = "保存全局配置（仅管理员；apiKey 必填，baseUrl/model 缺省用默认值）")
    @PostMapping
    public Result<Map<String, Object>> save(@RequestBody Map<String, String> body, HttpServletRequest request) {
        if (!isAdmin(request)) {
            return Result.error(403, "AI 连接由管理员统一配置，如有需要请联系管理员");
        }
        try {
            configService.saveGlobal(
                    body == null ? null : body.get("apiKey"),
                    body == null ? null : body.get("baseUrl"),
                    body == null ? null : body.get("model"),
                    body == null ? null : body.get("thinkingMode"));
            return Result.success(configService.globalStatus());
        } catch (IllegalArgumentException e) {
            return Result.error(400, e.getMessage());
        } catch (Exception e) {
            log.error("保存 AI 配置失败", e);
            return Result.error("保存失败，请稍后再试");
        }
    }

    @Operation(summary = "清除全局配置（仅管理员）")
    @DeleteMapping
    public Result<Map<String, Object>> clear(HttpServletRequest request) {
        if (!isAdmin(request)) {
            return Result.error(403, "AI 连接由管理员统一配置，如有需要请联系管理员");
        }
        configService.clearGlobal();
        return Result.success(configService.globalStatus());
    }

    @Operation(summary = "连接测试（apiKey 缺省时用已保存全局配置；仅管理员）")
    @PostMapping("/test")
    public Result<Map<String, Object>> test(@RequestBody(required = false) Map<String, String> body,
                                            HttpServletRequest request) {
        if (!isAdmin(request)) {
            return Result.error(403, "AI 连接由管理员统一配置，如有需要请联系管理员");
        }
        Map<String, Object> out = configService.test(
                body == null ? null : body.get("apiKey"),
                body == null ? null : body.get("baseUrl"),
                body == null ? null : body.get("model"));
        return Result.success(out);
    }
}
