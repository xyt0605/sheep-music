package com.example.sheepmusic.controller;

import com.example.sheepmusic.common.Result;
import com.example.sheepmusic.dto.RecommendItemVO;
import com.example.sheepmusic.entity.Playlist;
import com.example.sheepmusic.entity.Song;
import com.example.sheepmusic.service.RecommendationService;
import com.example.sheepmusic.utils.JwtUtil;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import java.util.List;

/**
 * 推荐系统控制器（v2：多路召回融合，响应携带推荐理由，见 docs/specs/推荐系统v2/）
 */
@Tag(name = "推荐系统")
@RestController
@RequestMapping("/music/recommend")
@CrossOrigin
public class RecommendationController {

    @Autowired
    private RecommendationService recommendationService;

    @Autowired
    private JwtUtil jwtUtil;

    /**
     * 获取个性化推荐歌曲（多路召回融合，携带推荐理由）
     */
    @Operation(summary = "获取个性化推荐歌曲")
    @GetMapping("/personalized")
    public Result<List<RecommendItemVO>> getPersonalizedSongs(
            HttpServletRequest request,
            @Parameter(description = "推荐数量", example = "20")
            @RequestParam(defaultValue = "20") int limit,
            @Parameter(description = "绕过缓存并采样换一批", example = "true")
            @RequestParam(defaultValue = "false") boolean refresh
    ) {
        try {
            Long userId = getUserIdFromToken(request);
            if (userId == null) {
                return Result.error("请先登录");
            }

            List<RecommendItemVO> recommendations = recommendationService.getPersonalizedSongs(userId, limit, refresh);
            return Result.success("推荐成功", recommendations);
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error("获取推荐失败: " + e.getMessage());
        }
    }

    /**
     * 猜你喜欢（混合推荐策略，携带推荐理由）
     */
    @Operation(summary = "猜你喜欢")
    @GetMapping("/guess-you-like")
    public Result<List<RecommendItemVO>> getGuessYouLike(
            HttpServletRequest request,
            @Parameter(description = "推荐数量", example = "30")
            @RequestParam(defaultValue = "30") int limit,
            @Parameter(description = "绕过缓存并采样换一批", example = "true")
            @RequestParam(defaultValue = "false") boolean refresh
    ) {
        try {
            Long userId = getUserIdFromToken(request);
            if (userId == null) {
                return Result.error("请先登录");
            }

            List<RecommendItemVO> recommendations = recommendationService.getGuessYouLike(userId, limit, refresh);
            return Result.success("推荐成功", recommendations);
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error("获取推荐失败: " + e.getMessage());
        }
    }
    
    /**
     * 获取相似歌曲（基于物品协同过滤）
     */
    @Operation(summary = "获取相似歌曲")
    @GetMapping("/similar-songs/{songId}")
    public Result<List<Song>> getSimilarSongs(
            @Parameter(description = "歌曲ID", required = true)
            @PathVariable Long songId,
            @Parameter(description = "推荐数量", example = "10")
            @RequestParam(defaultValue = "10") int limit
    ) {
        try {
            List<Song> similarSongs = recommendationService.getSimilarSongs(songId, limit);
            return Result.success("查询成功", similarSongs);
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error("获取相似歌曲失败: " + e.getMessage());
        }
    }
    
    /**
     * 推荐歌单
     */
    @Operation(summary = "推荐歌单")
    @GetMapping("/playlists")
    public Result<List<Playlist>> getRecommendedPlaylists(
            HttpServletRequest request,
            @Parameter(description = "推荐数量", example = "10")
            @RequestParam(defaultValue = "10") int limit
    ) {
        try {
            Long userId = getUserIdFromToken(request);
            if (userId == null) {
                return Result.error("请先登录");
            }
            
            List<Playlist> playlists = recommendationService.getRecommendedPlaylists(userId, limit);
            return Result.success("推荐成功", playlists);
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error("获取推荐歌单失败: " + e.getMessage());
        }
    }
    
    /**
     * 从请求头中获取用户ID
     */
    private Long getUserIdFromToken(HttpServletRequest request) {
        try {
            String token = request.getHeader("Authorization");
            if (token != null && token.startsWith("Bearer ")) {
                token = token.substring(7);
                return jwtUtil.getUserIdFromToken(token);
            }
            return null;
        } catch (Exception e) {
            return null;
        }
    }
}

