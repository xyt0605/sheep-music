package com.example.sheepmusic.agent;

import com.example.sheepmusic.dto.ExternalSearchResultVO;
import com.example.sheepmusic.dto.ExternalSongVO;
import com.example.sheepmusic.dto.RecommendItemVO;
import com.example.sheepmusic.entity.Artist;
import com.example.sheepmusic.entity.PlayHistory;
import com.example.sheepmusic.entity.Song;
import com.example.sheepmusic.repository.PlayHistoryRepository;
import com.example.sheepmusic.service.ExternalMusicService;
import com.example.sheepmusic.service.RecommendationService;
import com.example.sheepmusic.service.SongService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 小屋 DJ 的 Librarian 工具集（agent v1）
 * 全部为既有 Service 的薄包装，返回统一条目结构（ref 由编排器统一分配）
 */
@Slf4j
@Service
public class DjTools {

    @Autowired
    private SongService songService;

    @Autowired
    private RecommendationService recommendationService;

    @Autowired
    private ExternalMusicService externalMusicService;

    @Autowired
    private PlayHistoryRepository playHistoryRepository;

    /**
     * 口味画像（agent v1 P2）：播放历史近 50 条聚合常听歌手/风格 Top5 + 最近播放
     * 确定性数据、零 LLM 成本；失败返回空 Map（DJ 不提个性化）
     */
    public Map<String, Object> tasteProfile(Long userId) {
        Map<String, Object> profile = new LinkedHashMap<>();
        try {
            List<PlayHistory> plays = playHistoryRepository
                    .findByUserIdWithSongDetails(userId, PageRequest.of(0, 50))
                    .getContent();
            if (plays.isEmpty()) {
                return profile;
            }
            Map<String, Integer> artistCount = new HashMap<>();
            Map<String, Integer> genreCount = new HashMap<>();
            List<String> recentTitles = new ArrayList<>();
            for (PlayHistory ph : plays) {
                Song s = ph.getSong();
                if (s == null) {
                    continue;
                }
                for (Artist a : s.getArtists() == null ? List.<Artist>of() : s.getArtists()) {
                    artistCount.merge(a.getName(), 1, Integer::sum);
                }
                if (s.getGenre() != null && !s.getGenre().isBlank()) {
                    genreCount.merge(s.getGenre(), 1, Integer::sum);
                }
                if (recentTitles.size() < 5) {
                    recentTitles.add(s.getTitle());
                }
            }
            profile.put("topArtists", topN(artistCount, 5));
            profile.put("topGenres", topN(genreCount, 5));
            profile.put("recentTitles", recentTitles);
        } catch (Exception e) {
            log.warn("口味画像聚合失败: {}", e.getMessage());
        }
        return profile;
    }

    /** Map 按值降序取前 N，格式 "名称×次数" */
    private List<String> topN(Map<String, Integer> counts, int n) {
        return counts.entrySet().stream()
                .sorted((a, b) -> b.getValue() - a.getValue())
                .limit(n)
                .map(e -> e.getKey() + "×" + e.getValue())
                .toList();
    }

    /** search_local：本地曲库关键词检索 */
    public List<Map<String, Object>> searchLocal(String keyword, int limit) {
        Pageable pageable = PageRequest.of(0, clamp(limit, 1, 10));
        List<Map<String, Object>> out = new ArrayList<>();
        for (Song s : songService.searchSongs(keyword, pageable).getContent()) {
            if (s.getStatus() == null || s.getStatus() != 1) {
                continue;
            }
            Map<String, Object> item = item("local", s.getTitle());
            item.put("songId", s.getId());
            item.put("artist", artistNames(s));
            item.put("cover", s.getCover());
            item.put("genre", s.getGenre());
            item.put("language", s.getLanguage());
            item.put("url", s.getUrl());
            out.add(item);
        }
        return out;
    }

    /** recommend：推荐系统 v2 个性化通道（失败降级为空，Librarian 会自适应） */
    public List<Map<String, Object>> recommend(Long userId, int limit) {
        try {
            List<Map<String, Object>> out = new ArrayList<>();
            for (RecommendItemVO vo : recommendationService.getPersonalizedSongs(userId, clamp(limit, 1, 10), false)) {
                Song s = vo.getSong();
                if (s == null || (s.getStatus() != null && s.getStatus() != 1)) {
                    continue;
                }
                Map<String, Object> item = item("local", s.getTitle());
                item.put("songId", s.getId());
                item.put("artist", artistNames(s));
                item.put("cover", s.getCover());
                item.put("url", s.getUrl());
                item.put("reason", vo.getReason());
                out.add(item);
            }
            return out;
        } catch (Exception e) {
            log.warn("DJ recommend 工具失败: {}", e.getMessage());
            return List.of();
        }
    }

    /** search_web：歌曲海聚合源（曲库供应链 v1.2） */
    public List<Map<String, Object>> searchWeb(String keyword, int limit) {
        ExternalSearchResultVO result = externalMusicService.search("gequhai", keyword, 0, clamp(limit, 1, 10));
        List<Map<String, Object>> out = new ArrayList<>();
        for (ExternalSongVO vo : result.getItems()) {
            Map<String, Object> item = item("gequhai", vo.getTitle());
            item.put("sourceTrackId", vo.getSourceTrackId());
            item.put("artist", vo.getArtist());
            item.put("cover", vo.getCover());
            item.put("streamUrl", vo.getStreamUrl());
            item.put("isExternal", true);
            out.add(item);
        }
        return out;
    }

    private String artistNames(Song s) {
        if (s.getArtists() == null || s.getArtists().isEmpty()) {
            return "未知歌手";
        }
        return s.getArtists().stream().map(Artist::getName).reduce((a, b) -> a + " / " + b).orElse("未知歌手");
    }

    private Map<String, Object> item(String type, String title) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("type", type);
        m.put("title", title);
        return m;
    }

    private int clamp(int v, int min, int max) {
        return Math.min(Math.max(v, min), max);
    }
}
