package com.example.sheepmusic.controller;

import com.example.sheepmusic.common.Result;
import com.example.sheepmusic.entity.MemoryStar;
import com.example.sheepmusic.entity.User;
import com.example.sheepmusic.entity.MemoryItem;
import com.example.sheepmusic.repository.UserRepository;
import com.example.sheepmusic.service.MemoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 婉婉小屋管理端：素材 CRUD + 上下架 + 星星动态（仅管理员）
 * 安全：SecurityConfig 对 /memory/admin/** 要求 ADMIN 角色
 */
@Tag(name = "婉婉小屋管理")
@RestController
@RequestMapping("/memory/admin")
@CrossOrigin
@RequiredArgsConstructor
public class MemoryAdminController {

    private final MemoryService memoryService;
    private final UserRepository userRepo;

    @Operation(summary = "全量素材列表（含下架）")
    @GetMapping("/list")
    public Result<List<MemoryItem>> list() {
        return Result.success(memoryService.listAll());
    }

    @Operation(summary = "新建素材（媒体已上传 OSS，传 mediaUrl 引用）")
    @PostMapping
    public Result<MemoryItem> create(@RequestBody MemoryItem item) {
        try {
            if (item.getMediaUrl() == null || item.getMediaUrl().isBlank()) {
                return Result.error(400, "mediaUrl 不能为空");
            }
            if (item.getType() == null || !item.getType().matches("photo|video")) {
                return Result.error(400, "type 必须是 photo 或 video");
            }
            return Result.success("已保存", memoryService.create(item));
        } catch (IllegalArgumentException e) {
            return Result.error(400, e.getMessage());
        } catch (Exception e) {
            return Result.error("保存失败：" + e.getMessage());
        }
    }

    @Operation(summary = "编辑素材")
    @PutMapping("/{id}")
    public Result<MemoryItem> update(@PathVariable Long id, @RequestBody MemoryItem patch) {
        try {
            return Result.success("已保存", memoryService.update(id, patch));
        } catch (IllegalArgumentException e) {
            return Result.error(404, e.getMessage());
        } catch (Exception e) {
            return Result.error("保存失败：" + e.getMessage());
        }
    }

    @Operation(summary = "发布/下架（status: published/draft）")
    @PutMapping("/{id}/status")
    public Result<MemoryItem> changeStatus(@PathVariable Long id, @RequestParam String status) {
        try {
            return Result.success(memoryService.changeStatus(id, status));
        } catch (IllegalArgumentException e) {
            return Result.error(400, e.getMessage());
        }
    }

    @Operation(summary = "删除素材（DB + OSS 对象）")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        memoryService.delete(id);
        return Result.success("已删除", null);
    }

    @Operation(summary = "抱抱安全歌配置")
    @GetMapping("/hug-config")
    public Result<Map<String, Object>> hugConfig() {
        return Result.success(memoryService.getHugConfig());
    }

    @Operation(summary = "保存/清除抱抱安全歌（songId 与 songExternalId 都空 = 清除）")
    @PutMapping("/hug-config")
    public Result<Map<String, Object>> saveHugConfig(@RequestBody Map<String, Object> body) {
        try {
            Long songId = body.get("songId") == null ? null : Long.valueOf(String.valueOf(body.get("songId")));
            memoryService.saveHugConfig(
                    (String) body.get("songSource"),
                    songId,
                    (String) body.get("songExternalId"),
                    (String) body.get("songTitle"),
                    (String) body.get("songArtist"),
                    (String) body.get("songCover"));
            return Result.success("已保存", memoryService.getHugConfig());
        } catch (IllegalArgumentException e) {
            return Result.error(400, e.getMessage());
        }
    }

    @Operation(summary = "星星动态（近 14 天点亮记录）")
    @GetMapping("/star-feed")
    public Result<List<Map<String, Object>>> starFeed() {
        List<MemoryStar> stars = memoryService.starFeed();
        List<Map<String, Object>> feed = new ArrayList<>();
        for (MemoryStar s : stars) {
            Map<String, Object> row = new HashMap<>();
            row.put("id", s.getId());
            row.put("itemId", s.getItemId());
            row.put("userId", s.getUserId());
            row.put("createTime", s.getCreateTime());
            User u = userRepo.findById(s.getUserId()).orElse(null);
            row.put("nickname", u == null ? "已注销用户" : (u.getNickname() != null && !u.getNickname().isBlank() ? u.getNickname() : u.getUsername()));
            feed.add(row);
        }
        return Result.success(feed);
    }
}
