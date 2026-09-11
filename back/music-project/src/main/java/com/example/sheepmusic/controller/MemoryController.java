package com.example.sheepmusic.controller;

import com.example.sheepmusic.common.Result;
import com.example.sheepmusic.service.MemoryService;
import com.example.sheepmusic.utils.JwtUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 婉婉小屋展示端：列表 + 星星（登录用户）
 */
@Tag(name = "婉婉小屋")
@RestController
@RequestMapping("/memory")
@CrossOrigin
@RequiredArgsConstructor
public class MemoryController {

    private final MemoryService memoryService;
    private final JwtUtil jwtUtil;

    @Operation(summary = "小屋列表（按月分组 + 每日瞬间 + 我的星星数）")
    @GetMapping("/list")
    public Result<Map<String, Object>> list(HttpServletRequest request) {
        Long userId = jwtUtil.getUserIdFromRequest(request);
        return Result.success(memoryService.listForView(userId));
    }

    @Operation(summary = "点亮/取消星星（toggle）")
    @PostMapping("/star/{itemId}")
    public Result<Map<String, Object>> toggleStar(@PathVariable Long itemId, HttpServletRequest request) {
        Long userId = jwtUtil.getUserIdFromRequest(request);
        try {
            return Result.success(memoryService.toggleStar(itemId, userId));
        } catch (IllegalArgumentException e) {
            return Result.error(404, e.getMessage());
        }
    }
}
