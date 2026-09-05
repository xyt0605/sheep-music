package com.example.sheepmusic.service;

import com.example.sheepmusic.dto.RecommendItemVO;
import com.example.sheepmusic.entity.Favorite;
import com.example.sheepmusic.entity.Playlist;
import com.example.sheepmusic.entity.PlaylistSong;
import com.example.sheepmusic.entity.Song;
import com.example.sheepmusic.repository.FavoriteRepository;
import com.example.sheepmusic.repository.PlayHistoryRepository;
import com.example.sheepmusic.repository.PlaylistRepository;
import com.example.sheepmusic.repository.PlaylistSongRepository;
import com.example.sheepmusic.repository.SongRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * 推荐系统 v2 —— 多路召回 + 融合打分（规格见 docs/specs/推荐系统v2/）
 *
 * 召回通道：CF 物品协同 / 内容特征(genre+language) / 歌手维度 / 全局热度 / 新鲜度
 * 硬性约束：全部查询有界（无 findAll 全表扫描）、任何通道异常不影响整体接口（NFR-1/NFR-2）
 */
@Service
public class RecommendationService {

    // ==================== 可调参数（规格 NFR-3：集中定义，一处可调） ====================

    /** 通道权重：cf / content / artist / hot / fresh */
    private static final double W_CF = 0.50;
    private static final double W_CONTENT = 0.25;
    private static final double W_ARTIST = 0.15;
    private static final double W_HOT = 0.07;
    private static final double W_FRESH = 0.03;
    /** 猜你喜欢的权重（大盘混合，热度/新鲜度占比更高） */
    private static final double G_W_CF = 0.40;
    private static final double G_W_CONTENT = 0.20;
    private static final double G_W_ARTIST = 0.10;
    private static final double G_W_HOT = 0.20;
    private static final double G_W_FRESH = 0.10;

    private static final int CF_SEED_LIMIT = 20;          // CF 种子歌曲上限
    private static final int CF_CO_USER_LIMIT = 200;      // 共现用户上限
    private static final int PER_CHANNEL_LIMIT = 60;      // 单通道召回上限
    private static final int PROFILE_FAVORITE_LIMIT = 50; // 画像收藏上限
    private static final int CANDIDATE_LOAD_LIMIT = 150;  // 融合后批量加载歌曲实体的上限
    private static final int PLAYED_TOO_MUCH = 3;         // 听腻阈值：播放≥3次不再推荐
    private static final int COLD_START_MIN_SEEDS = 3;    // 冷启动种子数阈值
    private static final double DIVERSITY_MAIN_ARTIST_RATIO = 0.4; // 同主歌手占比上限

    private static final long CACHE_TTL_MS = 5 * 60 * 1000L; // 用户级缓存 TTL
    private static final int CACHE_MAX_ENTRIES = 1000;

    /** 策略标识 */
    private static final String S_CF = "cf";
    private static final String S_CONTENT = "content";
    private static final String S_ARTIST = "artist";
    private static final String S_HOT = "hot";
    private static final String S_FRESH = "fresh";

    @Autowired
    private FavoriteRepository favoriteRepository;

    @Autowired
    private PlayHistoryRepository playHistoryRepository;

    @Autowired
    private SongRepository songRepository;

    @Autowired
    private PlaylistRepository playlistRepository;

    @Autowired
    private PlaylistSongRepository playlistSongRepository;

    // ==================== 缓存（FR-5） ====================

    private static class CacheEntry {
        final List<RecommendItemVO> items;
        final long cachedAt;

        CacheEntry(List<RecommendItemVO> items) {
            this.items = items;
            this.cachedAt = System.currentTimeMillis();
        }

        boolean fresh() {
            return System.currentTimeMillis() - cachedAt < CACHE_TTL_MS;
        }
    }

    /** key: "userId:kind:limit" -> entry */
    private final ConcurrentHashMap<String, CacheEntry> cache = new ConcurrentHashMap<>();

    /**
     * 用户行为变化（收藏变更/新增播放）时失效该用户的推荐缓存
     */
    public void evictUserCache(Long userId) {
        cache.keySet().removeIf(key -> key.startsWith(userId + ":"));
    }

    // ==================== 对外入口 ====================

    /**
     * 个性化推荐（多路召回融合）
     */
    public List<RecommendItemVO> getPersonalizedSongs(Long userId, int limit, boolean refresh) {
        return recommend(userId, limit, refresh, false);
    }

    /**
     * 猜你喜欢（混合策略：热度/新鲜度占比更高）
     */
    public List<RecommendItemVO> getGuessYouLike(Long userId, int limit, boolean refresh) {
        return recommend(userId, limit, refresh, true);
    }

    /**
     * 相似歌曲（物品协同 + 内容特征，保持 List<Song> 兼容契约）
     */
    public List<Song> getSimilarSongs(Long songId, int limit) {
        List<Song> seedSongs = songRepository.findAllById(Collections.singletonList(songId));
        if (seedSongs.isEmpty() || seedSongs.get(0).getStatus() == null || seedSongs.get(0).getStatus() != 1) {
            return Collections.emptyList();
        }
        Song seed = seedSongs.get(0);

        // 有界的物品协同：共现用户 → 其喜爱歌曲聚合
        Map<Long, Double> scores = new HashMap<>();
        try {
            List<Favorite> seedFavs = favoriteRepository.findBySongIdIn(
                Collections.singletonList(songId), PageRequest.of(0, 500));
            List<Long> playUsers = playHistoryRepository.findUserIdsBySongIds(
                Collections.singletonList(songId), PageRequest.of(0, 300));
            Set<Long> coUsers = new HashSet<>();
            seedFavs.forEach(f -> coUsers.add(f.getUserId()));
            coUsers.addAll(playUsers);
            coUsers.remove(null);
            if (!coUsers.isEmpty()) {
                List<Favorite> coFavs = favoriteRepository.findByUserIdIn(coUsers, PageRequest.of(0, 500));
                for (Favorite f : coFavs) {
                    if (!f.getSongId().equals(songId)) {
                        scores.merge(f.getSongId(), 1.0, Double::sum);
                    }
                }
                List<Object[]> coPlays = playHistoryRepository.countPlaysGroupByUserIds(
                    coUsers, PageRequest.of(0, 1000));
                for (Object[] row : coPlays) {
                    Long sid = (Long) row[0];
                    Long cnt = (Long) row[1];
                    if (sid != null && !sid.equals(songId) && cnt != null) {
                        scores.merge(sid, Math.log1p(cnt) * 0.3, Double::sum);
                    }
                }
            }
        } catch (Exception ignored) {
            // NFR-2：通道异常不影响整体
        }

        // 内容特征兜底：同歌手 / 同风格
        try {
            List<Long> artistIds = seed.getArtists().stream()
                .map(a -> a.getId()).collect(Collectors.toList());
            if (!artistIds.isEmpty()) {
                songRepository.findByArtistIdsAndStatusOrderByPlayCountDesc(
                        artistIds, PageRequest.of(0, 30))
                    .stream().filter(s -> !s.getId().equals(songId))
                    .forEach(s -> scores.merge(s.getId(), 0.8, Double::sum));
            }
            if (seed.getGenre() != null && !seed.getGenre().isEmpty()) {
                songRepository.findByStatusAndGenreInOrderByPlayCountDesc(
                        1, Collections.singletonList(seed.getGenre()), PageRequest.of(0, 30))
                    .stream().filter(s -> !s.getId().equals(songId))
                    .forEach(s -> scores.merge(s.getId(), 0.5, Double::sum));
            }
        } catch (Exception ignored) {
        }

        List<Long> topIds = scores.entrySet().stream()
            .sorted(Map.Entry.<Long, Double>comparingByValue().reversed())
            .limit(limit)
            .map(Map.Entry::getKey)
            .collect(Collectors.toList());
        return getSongsInOrder(topIds);
    }

    /**
     * 推荐歌单（v2 保留既有实现，见规格 Out-of-Scope）
     */
    public List<Playlist> getRecommendedPlaylists(Long userId, int limit) {
        Set<Long> userLikedSongs = getUserLikedSongs(userId);

        if (userLikedSongs.isEmpty()) {
            return getHotPlaylists(limit);
        }

        List<Playlist> allPlaylists = playlistRepository.findByIsPublicOrderByCreateTimeDesc(true);

        Map<Long, Double> playlistScores = new HashMap<>();
        for (Playlist playlist : allPlaylists) {
            if (playlist.getUserId().equals(userId)) {
                continue;
            }
            if (playlist.getSongCount() == null || playlist.getSongCount() == 0) {
                continue;
            }
            List<PlaylistSong> playlistSongs = playlistSongRepository.findByPlaylistIdOrderByAddTimeDesc(playlist.getId());
            Set<Long> playlistSongIds = playlistSongs.stream()
                .map(PlaylistSong::getSongId)
                .collect(Collectors.toSet());
            if (playlistSongIds.isEmpty()) {
                continue;
            }
            Set<Long> intersection = new HashSet<>(userLikedSongs);
            intersection.retainAll(playlistSongIds);
            int matchCount = intersection.size();
            if (matchCount == 0) {
                continue;
            }
            double matchRatio = matchCount / Math.sqrt(userLikedSongs.size() * playlistSongIds.size());
            double popularityWeight = 1 + Math.log(1 + (playlist.getPlayCount() != null ? playlist.getPlayCount() : 0)
                + (playlist.getCollectCount() != null ? playlist.getCollectCount() : 0) * 2);
            playlistScores.put(playlist.getId(), matchRatio * popularityWeight);
        }

        if (playlistScores.isEmpty()) {
            return getHotPlaylists(limit);
        }

        List<Long> recommendedPlaylistIds = playlistScores.entrySet().stream()
            .sorted(Map.Entry.<Long, Double>comparingByValue().reversed())
            .limit(limit)
            .map(Map.Entry::getKey)
            .collect(Collectors.toList());

        List<Playlist> recommendedPlaylists = getPlaylistsInOrder(recommendedPlaylistIds);

        recommendedPlaylists.forEach(playlist -> {
            if (playlist.getCreator() != null) {
                playlist.getCreator().getNickname();
            }
        });

        return recommendedPlaylists;
    }

    // ==================== 推荐引擎核心 ====================

    private List<RecommendItemVO> recommend(Long userId, int limit, boolean refresh, boolean guessMode) {
        String cacheKey = userId + ":" + (guessMode ? "guess" : "pers") + ":" + limit;
        if (!refresh) {
            CacheEntry entry = cache.get(cacheKey);
            if (entry != null && entry.fresh()) {
                return entry.items;
            }
        }

        // refresh：绕过缓存，构建 3 倍候选池后随机采样 limit 条（规格 FR-5"换一批"）
        int buildLimit = refresh ? Math.min(limit * 3, CANDIDATE_LOAD_LIMIT) : limit;
        List<RecommendItemVO> result = buildRecommendation(userId, buildLimit, guessMode);
        if (refresh && result.size() > limit) {
            Collections.shuffle(result);
            result = new ArrayList<>(result.subList(0, limit));
        }

        if (!refresh && !result.isEmpty()) {
            if (cache.size() > CACHE_MAX_ENTRIES) {
                cache.clear(); // 简单防膨胀
            }
            cache.put(cacheKey, new CacheEntry(result));
        }
        return result;
    }

    private List<RecommendItemVO> buildRecommendation(Long userId, int limit, boolean guessMode) {
        // 0. 用户画像（全部有界查询）
        UserProfile profile = buildProfile(userId);

        // 1. 多路召回（每个候选在召回时就携带自己的理由，无跨请求共享状态）
        Map<Long, Double> combined = new HashMap<>();
        Map<Long, String> reasons = new HashMap<>();
        Map<Long, String> strategies = new HashMap<>();

        boolean coldStart = profile.isColdStart();
        if (!coldStart) {
            fuse(recallCf(userId, profile), S_CF, guessMode ? G_W_CF : W_CF, combined, reasons, strategies);
        }
        fuse(recallContent(profile), S_CONTENT, guessMode ? G_W_CONTENT : W_CONTENT, combined, reasons, strategies);
        if (!coldStart) {
            fuse(recallArtist(profile), S_ARTIST, guessMode ? G_W_ARTIST : W_ARTIST, combined, reasons, strategies);
        }
        fuse(recallHot(profile), S_HOT, guessMode ? G_W_HOT : W_HOT, combined, reasons, strategies);
        fuse(recallFresh(profile), S_FRESH, guessMode ? G_W_FRESH : W_FRESH, combined, reasons, strategies);

        // 2. 总分排序，截取候选池
        List<Long> rankedIds = combined.entrySet().stream()
            .sorted(Map.Entry.<Long, Double>comparingByValue().reversed())
            .limit(CANDIDATE_LOAD_LIMIT)
            .map(Map.Entry::getKey)
            .collect(Collectors.toList());

        // 3. 批量加载歌曲实体（有界），硬过滤（规格 FR-2）
        Map<Long, Song> songById = songRepository.findAllById(rankedIds).stream()
            .collect(Collectors.toMap(Song::getId, s -> s, (a, b) -> a));

        List<Song> filtered = rankedIds.stream()
            .map(songById::get)
            .filter(Objects::nonNull)
            .filter(s -> s.getStatus() != null && s.getStatus() == 1)
            .filter(s -> !profile.getFavoriteSongIds().contains(s.getId()))
            .filter(s -> profile.getPlayCounts().getOrDefault(s.getId(), 0L) < PLAYED_TOO_MUCH)
            .collect(Collectors.toList());

        // 4. 多样性打散（FR-3）
        List<Song> diversified = diversifyByMainArtist(filtered, limit);

        // 5. 组装 VO
        return diversified.stream()
            .limit(limit)
            .map(s -> new RecommendItemVO(
                s,
                round2(combined.getOrDefault(s.getId(), 0.0)),
                reasons.getOrDefault(s.getId(), "根据你的口味推荐"),
                strategies.getOrDefault(s.getId(), S_HOT)))
            .collect(Collectors.toList());
    }

    /**
     * 通道融合：通道内按最高分归一化后乘以通道权重累加进 combined；
     * 首个召回该歌曲的通道获得理由归属（通道优先级 = 调用顺序：cf > content > artist > hot > fresh）
     */
    private void fuse(Map<Long, RawRec> raw, String strategy, double weight,
                      Map<Long, Double> combined, Map<Long, String> reasons, Map<Long, String> strategies) {
        if (raw == null || raw.isEmpty()) {
            return;
        }
        double max = raw.values().stream().mapToDouble(r -> r.score).max().orElse(0);
        if (max <= 0) {
            return;
        }
        for (Map.Entry<Long, RawRec> e : raw.entrySet()) {
            if (e.getValue().score <= 0) {
                continue;
            }
            combined.merge(e.getKey(), e.getValue().score / max * weight, Double::sum);
            reasons.putIfAbsent(e.getKey(), e.getValue().reason);
            strategies.putIfAbsent(e.getKey(), strategy);
        }
    }

    // ==================== 召回通道 ====================

    /** 单通道内的一条召回：原始分 + 理由 */
    private static class RawRec {
        final double score;
        final String reason;

        RawRec(double score, String reason) {
            this.score = score;
            this.reason = reason;
        }
    }

    /**
     * CH-CF 物品协同：种子歌曲 → 共现用户（收藏+播放）→ 共现用户的喜爱歌曲聚合。
     * 全程有界：种子≤20、共现用户≤200、中间结果均有 LIMIT。
     */
    private Map<Long, RawRec> recallCf(Long userId, UserProfile profile) {
        Map<Long, RawRec> out = new HashMap<>();
        try {
            List<Long> seedIds = topSeedIds(profile, CF_SEED_LIMIT);
            if (seedIds.isEmpty()) {
                return out;
            }
            Map<Long, Song> seedSongs = profile.getSeedSongs();

            // 共现用户：收藏过种子的用户 + 播过种子的用户
            List<Favorite> seedFavs = favoriteRepository.findBySongIdIn(seedIds, PageRequest.of(0, 1000));
            List<Long> playUsers = playHistoryRepository.findUserIdsBySongIds(
                seedIds, PageRequest.of(0, CF_CO_USER_LIMIT));

            Map<Long, Set<Long>> userSeeds = new HashMap<>(); // 共现用户 -> 其听过的种子
            for (Favorite f : seedFavs) {
                if (!f.getUserId().equals(userId) && seedIds.contains(f.getSongId())) {
                    userSeeds.computeIfAbsent(f.getUserId(), k -> new HashSet<>()).add(f.getSongId());
                }
            }
            for (Long uid : playUsers) {
                if (uid != null && !uid.equals(userId)) {
                    userSeeds.computeIfAbsent(uid, k -> new HashSet<>());
                }
            }
            List<Long> coUsers = userSeeds.keySet().stream()
                .limit(CF_CO_USER_LIMIT).collect(Collectors.toList());
            if (coUsers.isEmpty()) {
                return out;
            }

            // 共现用户的喜爱歌曲（收藏）与播放聚合
            List<Favorite> coFavs = favoriteRepository.findByUserIdIn(coUsers, PageRequest.of(0, 800));
            List<Object[]> coPlays = playHistoryRepository.countPlaysGroupByUserIds(
                coUsers, PageRequest.of(0, 1000));

            for (Favorite f : coFavs) {
                Long songId = f.getSongId();
                if (seedIds.contains(songId)) {
                    continue; // 排除种子本身
                }
                out.merge(songId, new RawRec(1.0, null), (a, b) ->
                    new RawRec(a.score + b.score, a.reason != null ? a.reason : b.reason));
                attachCfReason(out, songId, userSeeds.get(f.getUserId()), profile);
            }
            for (Object[] row : coPlays) {
                Long songId = (Long) row[0];
                Long cnt = (Long) row[1];
                if (songId == null || cnt == null || seedIds.contains(songId)) {
                    continue;
                }
                out.merge(songId, new RawRec(Math.log1p(cnt) * 0.3, null), (a, b) ->
                    new RawRec(a.score + b.score, a.reason != null ? a.reason : b.reason));
                attachCfReason(out, songId, null, profile);
            }
        } catch (Exception ignored) {
            // NFR-2：通道异常不影响整体
        }
        return out;
    }

    /** 为 CF 候选补充理由：取该共现用户听过的种子里权重最高的一首歌名 */
    private void attachCfReason(Map<Long, RawRec> out, Long songId,
                                Set<Long> userSeedIds, UserProfile profile) {
        RawRec rec = out.get(songId);
        if (rec == null || rec.reason != null) {
            return;
        }
        List<Long> rankedSeeds = topSeedIds(profile, CF_SEED_LIMIT);
        String title = null;
        if (userSeedIds != null && !userSeedIds.isEmpty()) {
            title = rankedSeeds.stream()
                .filter(userSeedIds::contains)
                .map(profile.getSeedSongs()::get)
                .filter(Objects::nonNull)
                .map(Song::getTitle)
                .findFirst()
                .orElse(null);
        }
        if (title == null && !rankedSeeds.isEmpty()) {
            Song topSeed = profile.getSeedSongs().get(rankedSeeds.get(0));
            title = topSeed == null ? null : topSeed.getTitle();
        }
        if (title != null) {
            out.put(songId, new RawRec(rec.score, "听过《" + title + "》的人也在听"));
        }
    }

    /**
     * CH-CONTENT 内容特征：偏好风格（主信号）+ 语言（弱信号）
     */
    private Map<Long, RawRec> recallContent(UserProfile profile) {
        Map<Long, RawRec> out = new HashMap<>();
        try {
            Map<String, Double> genreWeights = profile.getGenreWeights();
            if (!genreWeights.isEmpty()) {
                double maxG = genreWeights.values().stream().mapToDouble(Double::doubleValue).max().orElse(1);
                List<String> topGenres = genreWeights.entrySet().stream()
                    .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
                    .limit(3).map(Map.Entry::getKey).collect(Collectors.toList());
                List<Song> songs = songRepository.findByStatusAndGenreInOrderByPlayCountDesc(
                    1, topGenres, PageRequest.of(0, PER_CHANNEL_LIMIT));
                long maxPlay = songs.stream().mapToLong(s -> nvl(s.getPlayCount())).max().orElse(1);
                for (Song s : songs) {
                    Double gw = genreWeights.get(s.getGenre());
                    if (gw == null) {
                        continue;
                    }
                    double score = (gw / maxG) * (0.5 + 0.5 * nvl(s.getPlayCount()) / (double) Math.max(maxPlay, 1));
                    out.merge(s.getId(), new RawRec(score, "与你常听的「" + s.getGenre() + "」风格相符"),
                        RecommendationService::mergeRec);
                }
            }
            if (!profile.getLanguageWeights().isEmpty()) {
                List<String> topLangs = profile.getLanguageWeights().keySet().stream()
                    .limit(2).collect(Collectors.toList());
                songRepository.findByStatusAndLanguageInOrderByPlayCountDesc(
                        1, topLangs, PageRequest.of(0, 30))
                    .forEach(s -> out.merge(s.getId(),
                        new RawRec(0.2, "与你常听的语言相符"), RecommendationService::mergeRec));
            }
        } catch (Exception ignored) {
        }
        return out;
    }

    /**
     * CH-ARTIST 歌手维度：偏好歌手的其它歌曲
     */
    private Map<Long, RawRec> recallArtist(UserProfile profile) {
        Map<Long, RawRec> out = new HashMap<>();
        try {
            Map<Long, Double> artistWeights = profile.getArtistWeights();
            List<Long> topArtistIds = artistWeights.entrySet().stream()
                .sorted(Map.Entry.<Long, Double>comparingByValue().reversed())
                .limit(3).map(Map.Entry::getKey).collect(Collectors.toList());
            if (topArtistIds.isEmpty()) {
                return out;
            }
            double maxAw = artistWeights.values().stream().mapToDouble(Double::doubleValue).max().orElse(1);
            List<Song> songs = songRepository.findByArtistIdsAndStatusOrderByPlayCountDesc(
                topArtistIds, PageRequest.of(0, PER_CHANNEL_LIMIT));
            long maxPlay = songs.stream().mapToLong(s -> nvl(s.getPlayCount())).max().orElse(1);
            for (Song s : songs) {
                double aw = s.getArtists().stream()
                    .filter(a -> artistWeights.containsKey(a.getId()))
                    .mapToDouble(a -> artistWeights.get(a.getId()))
                    .max().orElse(0);
                if (aw <= 0) {
                    continue;
                }
                double score = (aw / maxAw) * (0.5 + 0.5 * nvl(s.getPlayCount()) / (double) Math.max(maxPlay, 1));
                String artistName = s.getArtists().stream()
                    .filter(a -> artistWeights.containsKey(a.getId()))
                    .map(a -> a.getName())
                    .findFirst().orElse("你常听的歌手");
                out.merge(s.getId(), new RawRec(score, "你常听「" + artistName + "」，试试这首歌"),
                    RecommendationService::mergeRec);
            }
        } catch (Exception ignored) {
        }
        return out;
    }

    /**
     * CH-HOT 全局热度（排除种子歌曲）
     */
    private Map<Long, RawRec> recallHot(UserProfile profile) {
        Map<Long, RawRec> out = new HashMap<>();
        try {
            songRepository.findByStatusOrderByPlayCountDesc(1, PageRequest.of(0, PER_CHANNEL_LIMIT))
                .forEach(s -> {
                    if (!profile.getSeedWeights().containsKey(s.getId())) {
                        out.put(s.getId(), new RawRec(nvl(s.getPlayCount()) + 1.0, "全网都在循环"));
                    }
                });
        } catch (Exception ignored) {
        }
        return out;
    }

    /**
     * CH-FRESH 新鲜度：近 30 天创建的上架歌曲
     */
    private Map<Long, RawRec> recallFresh(UserProfile profile) {
        Map<Long, RawRec> out = new HashMap<>();
        try {
            LocalDateTime since = LocalDateTime.now().minusDays(30);
            songRepository.findByStatusAndCreateTimeAfterOrderByPlayCountDesc(
                    1, since, PageRequest.of(0, 30))
                .forEach(s -> {
                    if (!profile.getSeedWeights().containsKey(s.getId())) {
                        out.put(s.getId(), new RawRec(nvl(s.getPlayCount()) + 1.0, "近 30 天的新鲜好歌"));
                    }
                });
        } catch (Exception ignored) {
        }
        return out;
    }

    private static RawRec mergeRec(RawRec a, RawRec b) {
        return new RawRec(a.score + b.score, a.reason != null ? a.reason : b.reason);
    }

    // ==================== 用户画像 ====================

    private UserProfile buildProfile(Long userId) {
        UserProfile p = new UserProfile();

        // 收藏（有界：≤50）
        List<Favorite> favs = favoriteRepository.findByUserIdOrderByCreateTimeDesc(
            userId, PageRequest.of(0, PROFILE_FAVORITE_LIMIT)).getContent();
        p.getFavoriteSongIds().addAll(favs.stream().map(Favorite::getSongId).collect(Collectors.toList()));

        // 播放聚合（GROUP BY 一次查询，附带最近播放时间用于时间衰减）
        Map<Long, Long> playCounts = new HashMap<>();
        Map<Long, LocalDateTime> lastPlay = new HashMap<>();
        try {
            List<Object[]> rows = playHistoryRepository.countPlaysGroupBySong(userId);
            if (rows != null) {
                for (Object[] row : rows) {
                    Long songId = (Long) row[0];
                    Long cnt = (Long) row[1];
                    LocalDateTime last = (LocalDateTime) row[2];
                    if (songId == null || cnt == null) {
                        continue;
                    }
                    playCounts.put(songId, cnt);
                    if (last != null) {
                        lastPlay.put(songId, last);
                    }
                }
            }
        } catch (Exception ignored) {
        }
        p.setPlayCounts(playCounts);

        // 种子权重：收藏 2.0 + ln(1+播放次数)，按最近播放时间衰减（规格 FR-1）
        Set<Long> seedIds = new LinkedHashSet<>(p.getFavoriteSongIds());
        seedIds.addAll(playCounts.keySet());
        Map<Long, Song> seedSongs = seedIds.isEmpty()
            ? Collections.emptyMap()
            : songRepository.findAllById(seedIds).stream()
                .collect(Collectors.toMap(Song::getId, s -> s, (a, b) -> a));
        p.setSeedSongs(seedSongs);

        LocalDateTime now = LocalDateTime.now();
        for (Long songId : seedIds) {
            double w = 0;
            if (p.getFavoriteSongIds().contains(songId)) {
                w += 2.0;
            }
            Long cnt = playCounts.get(songId);
            if (cnt != null) {
                w += Math.log1p(cnt);
            }
            w *= recencyDecay(lastPlay.get(songId), now);
            if (w > 0) {
                p.getSeedWeights().put(songId, w);
            }
        }

        // 内容画像：风格/语言/歌手权重（来自种子歌曲实体）
        for (Map.Entry<Long, Double> e : p.getSeedWeights().entrySet()) {
            Song s = seedSongs.get(e.getKey());
            if (s == null) {
                continue;
            }
            if (s.getGenre() != null && !s.getGenre().isEmpty()) {
                p.getGenreWeights().merge(s.getGenre(), e.getValue(), Double::sum);
            }
            if (s.getLanguage() != null && !s.getLanguage().isEmpty()) {
                p.getLanguageWeights().merge(s.getLanguage(), e.getValue(), Double::sum);
            }
            s.getArtists().forEach(a -> p.getArtistWeights().merge(a.getId(), e.getValue(), Double::sum));
        }

        p.setColdStart(p.getSeedWeights().size() < COLD_START_MIN_SEEDS);
        return p;
    }

    /** 播放时间衰减：7 天内 1.0，30 天 0.6，90 天以上 0.3（规格 FR-1） */
    private double recencyDecay(LocalDateTime lastPlay, LocalDateTime now) {
        if (lastPlay == null) {
            return 0.6; // 无播放时间（纯收藏）给中间值
        }
        long days = Duration.between(lastPlay, now).toDays();
        if (days <= 7) {
            return 1.0;
        }
        if (days <= 30) {
            return 0.6;
        }
        return 0.3;
    }

    // ==================== 工具 ====================

    /**
     * 多样性打散：同一主歌手歌曲数 ≤ ceil(limit × 0.4)，贪心保留高分项
     */
    private List<Song> diversifyByMainArtist(List<Song> sorted, int limit) {
        int cap = (int) Math.ceil(limit * DIVERSITY_MAIN_ARTIST_RATIO);
        Map<Long, Integer> perArtist = new HashMap<>();
        List<Song> result = new ArrayList<>();
        List<Song> overflow = new ArrayList<>();
        for (Song s : sorted) {
            Long mainArtist = mainArtistId(s);
            int used = perArtist.getOrDefault(mainArtist, 0);
            if (used < cap) {
                perArtist.put(mainArtist, used + 1);
                result.add(s);
            } else {
                overflow.add(s);
            }
            if (result.size() >= limit) {
                break;
            }
        }
        for (Song s : overflow) {
            if (result.size() >= limit) {
                break;
            }
            Long mainArtist = mainArtistId(s);
            int used = perArtist.getOrDefault(mainArtist, 0);
            if (used < cap) {
                perArtist.put(mainArtist, used + 1);
                result.add(s);
            }
        }
        return result;
    }

    private Long mainArtistId(Song s) {
        if (s.getArtists() == null || s.getArtists().isEmpty()) {
            return -1L;
        }
        return s.getArtists().get(0).getId();
    }

    private List<Long> topSeedIds(UserProfile profile, int k) {
        return profile.getSeedWeights().entrySet().stream()
            .sorted(Map.Entry.<Long, Double>comparingByValue().reversed())
            .limit(k)
            .map(Map.Entry::getKey)
            .collect(Collectors.toList());
    }

    /** 按给定ID顺序回查歌曲并过滤下架（findAllById 不保证顺序；ids 有界） */
    private List<Song> getSongsInOrder(List<Long> songIds) {
        if (songIds == null || songIds.isEmpty()) {
            return new ArrayList<>();
        }
        Map<Long, Song> songById = songRepository.findAllById(songIds).stream()
            .collect(Collectors.toMap(Song::getId, s -> s, (a, b) -> a));
        return songIds.stream()
            .map(songById::get)
            .filter(Objects::nonNull)
            .filter(song -> song.getStatus() != null && song.getStatus() == 1)
            .collect(Collectors.toList());
    }

    private List<Playlist> getPlaylistsInOrder(List<Long> playlistIds) {
        if (playlistIds == null || playlistIds.isEmpty()) {
            return new ArrayList<>();
        }
        Map<Long, Playlist> playlistById = playlistRepository.findAllById(playlistIds).stream()
            .collect(Collectors.toMap(Playlist::getId, p -> p, (a, b) -> a));
        return playlistIds.stream()
            .map(playlistById::get)
            .filter(Objects::nonNull)
            .collect(Collectors.toList());
    }

    private double round2(double v) {
        return Math.round(v * 100.0) / 100.0;
    }

    private long nvl(Long v) {
        return v == null ? 0L : v;
    }

    /** 用户喜爱的歌曲ID集合（歌单推荐用） */
    private Set<Long> getUserLikedSongs(Long userId) {
        Set<Long> likedSongs = new HashSet<>();
        List<Favorite> favorites = favoriteRepository.findByUserIdOrderByCreateTimeDesc(userId);
        likedSongs.addAll(favorites.stream().map(Favorite::getSongId).collect(Collectors.toSet()));
        return likedSongs;
    }

    private List<Playlist> getHotPlaylists(int limit) {
        List<Playlist> hotPlaylists = playlistRepository.findByIsPublicOrderByCreateTimeDesc(true)
            .stream()
            .sorted(Comparator.comparing(Playlist::getSongCount).reversed())
            .limit(limit)
            .collect(Collectors.toList());
        hotPlaylists.forEach(playlist -> {
            if (playlist.getCreator() != null) {
                playlist.getCreator().getNickname();
            }
        });
        return hotPlaylists;
    }

    /**
     * 用户画像（一次推荐构建的内部上下文，不跨请求共享）
     */
    private static class UserProfile {
        /** songId -> 种子权重（收藏2.0 + ln(1+播放)，已含时间衰减） */
        private final Map<Long, Double> seedWeights = new HashMap<>();
        /** songId -> 播放次数 */
        private Map<Long, Long> playCounts = new HashMap<>();
        /** 用户收藏的歌曲ID */
        private final List<Long> favoriteSongIds = new ArrayList<>();
        /** 种子歌曲实体（用于取风格/语言/歌手与理由文案） */
        private Map<Long, Song> seedSongs = Collections.emptyMap();
        /** 风格权重画像 */
        private final Map<String, Double> genreWeights = new HashMap<>();
        /** 语言权重画像 */
        private final Map<String, Double> languageWeights = new HashMap<>();
        /** 歌手权重画像 */
        private final Map<Long, Double> artistWeights = new HashMap<>();
        /** 是否冷启动 */
        private boolean coldStart;

        Map<Long, Double> getSeedWeights() { return seedWeights; }
        Map<Long, Long> getPlayCounts() { return playCounts; }
        void setPlayCounts(Map<Long, Long> v) { this.playCounts = v; }
        List<Long> getFavoriteSongIds() { return favoriteSongIds; }
        Map<Long, Song> getSeedSongs() { return seedSongs; }
        void setSeedSongs(Map<Long, Song> v) { this.seedSongs = v; }
        Map<String, Double> getGenreWeights() { return genreWeights; }
        Map<String, Double> getLanguageWeights() { return languageWeights; }
        Map<Long, Double> getArtistWeights() { return artistWeights; }
        boolean isColdStart() { return coldStart; }
        void setColdStart(boolean v) { this.coldStart = v; }
    }
}
