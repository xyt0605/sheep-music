package com.example.sheepmusic.controller;

import com.example.sheepmusic.common.Result;
import com.example.sheepmusic.service.SystemConfigService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 系统设置（系统设置 v1）：仅管理员
 * 安全：/system/config/** 在 SecurityConfig 中要求 ADMIN 角色
 */
@Slf4j
@Tag(name = "系统设置")
@RestController
@RequestMapping("/system/config")
@CrossOrigin
@RequiredArgsConstructor
public class SystemConfigController {

    private final SystemConfigService systemConfigService;

    @Operation(summary = "OSS 配置状态（密钥脱敏）")
    @GetMapping("/oss")
    public Result<Map<String, Object>> ossStatus() {
        return Result.success(systemConfigService.ossStatus());
    }

    @Operation(summary = "保存 OSS 配置（Secret 留空沿用旧值；运行时生效）")
    @PutMapping("/oss")
    public Result<Map<String, Object>> saveOss(@RequestBody Map<String, String> body) {
        try {
            systemConfigService.saveOss(
                    body.get("endpoint"),
                    body.get("accessKeyId"),
                    body.get("accessKeySecret"),
                    body.get("bucketName"),
                    body.get("urlPrefix"));
            return Result.success("已保存，即刻生效", systemConfigService.ossStatus());
        } catch (IllegalArgumentException e) {
            return Result.error(400, e.getMessage());
        } catch (Exception e) {
            log.error("保存 OSS 配置失败", e);
            return Result.error("保存失败，请稍后再试");
        }
    }

    @Operation(summary = "测试 OSS 连接（可传覆盖值，不落库）")
    @PostMapping("/oss/test")
    public Result<Map<String, Object>> testOss(@RequestBody(required = false) Map<String, String> body) {
        return Result.success(systemConfigService.testOss(body));
    }

    @Operation(summary = "恢复默认（删除面板配置，回落 application.yml）")
    @DeleteMapping("/oss")
    public Result<Map<String, Object>> resetOss() {
        systemConfigService.resetOss();
        return Result.success("已恢复默认配置", systemConfigService.ossStatus());
    }
}
