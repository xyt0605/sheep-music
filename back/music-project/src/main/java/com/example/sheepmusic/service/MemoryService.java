package com.example.sheepmusic.service;

import com.example.sheepmusic.entity.Artist;
import com.example.sheepmusic.entity.MemoryItem;
import com.example.sheepmusic.entity.MemoryStar;
import com.example.sheepmusic.entity.Song;
import com.example.sheepmusic.entity.SystemConfig;
import com.example.sheepmusic.entity.User;
import com.example.sheepmusic.repository.MemoryItemRepository;
import com.example.sheepmusic.repository.MemoryStarRepository;
import com.example.sheepmusic.repository.SongRepository;
import com.example.sheepmusic.repository.SystemConfigRepository;
import com.example.sheepmusic.repository.UserRepository;
import com.example.sheepmusic.service.NotificationService;
import com.example.sheepmusic.utils.OSSUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 婉婉小屋服务：todayPick 确定性挑选、星星 toggle、管理 CRUD + OSS 联动
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MemoryService {

    private final MemoryItemRepository itemRepo;
    private final MemoryStarRepository starRepo;
    private final SongRepository songRepo;
    private final OSSUtil ossUtil;
    private final SystemConfigRepository systemConfigRepo;
    private final UserRepository userRepo;
    private final NotificationService notificationService;

    /**
     * 展示页列表：published 素材按月分组 + todayPick + 我的星星数
     */
    public Map<String, Object> listForView(Long userId) {
        List<MemoryItem> items = itemRepo.findByStatusOrderByMemoryDateDescSortOrderDescIdDesc("published");

        // 按月分组（YYYY-MM，新→旧）；memoryDate 空则落到上传日
        Map<String, List<MemoryItem>> byMonth = new LinkedHashMap<>();
        for (MemoryItem it : items) {
            LocalDate d = it.getMemoryDate() != null ? it.getMemoryDate() : it.getCreateTime().toLocalDate();
            byMonth.computeIfAbsent(d.getYear() + "-" + String.format("%02d", d.getMonthValue()), k -> new ArrayList<>()).add(it);
        }
        List<Map<String, Object>> groups = new ArrayList<>();
        for (Map.Entry<String, List<MemoryItem>> e : byMonth.entrySet()) {
            groups.add(Map.of("month", e.getKey(), "items", e.getValue()));
        }

        Map<String, Object> out = new LinkedHashMap<>();
        out.put("groups", groups);
        out.put("total", items.size());
        out.put("myStarCount", starRepo.countByUserId(userId));
        out.put("todayPick", pickToday(items));
        return out;
    }

    /**
     * 每日瞬间：epochDay 对列表长度取模——同一天稳定，跨天轮换
     */
    private MemoryItem pickToday(List<MemoryItem> items) {
        if (items == null || items.isEmpty()) {
            return null;
        }
        long epochDay = LocalDate.now().toEpochDay();
        return items.get((int) (epochDay % items.size()));
    }

    /**
     * 点亮/取消星星（toggle），返回最新状态
     */
    @Transactional
    public Map<String, Object> toggleStar(Long itemId, Long userId) {
        MemoryItem item = itemRepo.findById(itemId).orElse(null);
        if (item == null) {
            throw new IllegalArgumentException("素材不存在或已删除");
        }
        Optional<MemoryStar> existing = starRepo.findByItemIdAndUserId(itemId, userId);
        boolean starred;
        if (existing.isPresent()) {
            starRepo.delete(existing.get());
            item.setStarCount(Math.max(0, (item.getStarCount() == null ? 0 : item.getStarCount()) - 1));
            starred = false;
        } else {
            MemoryStar star = new MemoryStar();
            star.setItemId(itemId);
            star.setUserId(userId);
            starRepo.save(star);
            item.setStarCount((item.getStarCount() == null ? 0 : item.getStarCount()) + 1);
            starred = true;
        }
        itemRepo.save(item);
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("starred", starred);
        out.put("starCount", item.getStarCount());
        out.put("myStarCount", starRepo.countByUserId(userId));
        return out;
    }

    // ===== 管理端 =====

    public List<MemoryItem> listAll() {
        // 管理端含 draft（全量），排序与展示页一致
        return itemRepo.findByOrderByMemoryDateDescSortOrderDescIdDesc();
    }

    /**
     * 新建素材（媒体已先行上传 OSS，这里只落库）；绑定歌曲时回填冗余字段
     */
    @Transactional
    public MemoryItem create(MemoryItem item) {
        normalizeSongFields(item);
        if (item.getStatus() == null || item.getStatus().isBlank()) {
            item.setStatus("published");
        }
        return itemRepo.save(item);
    }

    @Transactional
    public MemoryItem update(Long id, MemoryItem patch) {
        MemoryItem item = itemRepo.findById(id).orElse(null);
        if (item == null) {
            throw new IllegalArgumentException("素材不存在");
        }
        if (patch.getType() != null) item.setType(patch.getType());
        if (patch.getMediaUrl() != null) item.setMediaUrl(patch.getMediaUrl());
        item.setCoverUrl(patch.getCoverUrl());
        item.setTitle(patch.getTitle());
        item.setCaption(patch.getCaption());
        item.setMemoryDate(patch.getMemoryDate());
        item.setSongId(patch.getSongId());
        item.setSongSource(patch.getSongSource());
        item.setSongExternalId(patch.getSongExternalId());
        // 冗余字段先照抄 patch：local 来源随后由 normalizeSongFields 用曲库数据覆盖，gequhai 保留面板传来的展示信息
        item.setSongTitle(patch.getSongTitle());
        item.setSongArtist(patch.getSongArtist());
        item.setSongCover(patch.getSongCover());
        item.setSortOrder(patch.getSortOrder() == null ? 0 : patch.getSortOrder());
        if (patch.getStatus() != null && !patch.getStatus().isBlank()) item.setStatus(patch.getStatus());
        normalizeSongFields(item);
        return itemRepo.save(item);
    }

    /**
     * 发布/下架
     */
    @Transactional
    public MemoryItem changeStatus(Long id, String status) {
        if (!"published".equals(status) && !"draft".equals(status)) {
            throw new IllegalArgumentException("非法状态");
        }
        MemoryItem item = itemRepo.findById(id).orElse(null);
        if (item == null) {
            throw new IllegalArgumentException("素材不存在");
        }
        item.setStatus(status);
        return itemRepo.save(item);
    }

    /**
     * 删除素材：DB 行 + OSS 媒体/封面对象；OSS 删除失败仅记日志不阻断
     */
    @Transactional
    public void delete(Long id) {
        MemoryItem item = itemRepo.findById(id).orElse(null);
        if (item == null) {
            return;
        }
        itemRepo.deleteById(id);
        starRepo.deleteAll(starRepo.findByItemId(id));
        try {
            ossUtil.deleteFile(item.getMediaUrl());
            if (item.getCoverUrl() != null && !item.getCoverUrl().isBlank()) {
                ossUtil.deleteFile(item.getCoverUrl());
            }
        } catch (Exception e) {
            log.warn("删除 OSS 文件失败（不阻断）memoryId={} url={}", id, item.getMediaUrl(), e);
        }
    }

    /**
     * 星星动态（近 14 天）：点亮时间 + 素材标题 + 点亮人昵称（由 controller 组装）
     */
    public List<MemoryStar> starFeed() {
        return starRepo.findByCreateTimeAfterOrderByCreateTimeDesc(LocalDateTime.now().minusDays(14));
    }

    // ===== v1.2：星图 + 抱抱按钮 =====

    /**
     * 星图：当前用户的全部星星 + 关联素材信息（缩略图/配文/日期），按点亮时间升序
     */
    public List<Map<String, Object>> starMap(Long userId) {
        List<Map<String, Object>> out = new ArrayList<>();
        int i = 0;
        for (MemoryStar s : starRepo.findByUserId(userId)) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("starId", s.getId());
            row.put("index", i++);
            row.put("createTime", s.getCreateTime());
            MemoryItem item = itemRepo.findById(s.getItemId()).orElse(null);
            if (item == null || !"published".equals(item.getStatus())) {
                continue; // 素材已删/下架：星星失去落点，星图不显示
            }
            row.put("itemId", item.getId());
            row.put("type", item.getType());
            row.put("thumb", "photo".equals(item.getType()) ? item.getMediaUrl() : item.getCoverUrl());
            row.put("title", item.getTitle());
            row.put("caption", item.getCaption());
            row.put("memoryDate", item.getMemoryDate() != null ? item.getMemoryDate().toString()
                    : item.getCreateTime().toLocalDate().toString());
            out.add(row);
        }
        return out;
    }

    /** 抱抱安全歌配置（hug.* 键，存 tb_system_config，非密钥明文） */
    public Map<String, Object> getHugConfig() {
        Map<String, String> rows = hugRows();
        Map<String, Object> out = new LinkedHashMap<>();
        boolean enabled = rows.get("hug.songTitle") != null && !rows.get("hug.songTitle").isBlank();
        out.put("configured", enabled);
        if (enabled) {
            out.put("songSource", rows.getOrDefault("hug.songSource", "local"));
            out.put("songId", rows.get("hug.songId"));
            out.put("songExternalId", rows.get("hug.songExternalId"));
            out.put("songTitle", rows.get("hug.songTitle"));
            out.put("songArtist", rows.get("hug.songArtist"));
            out.put("songCover", rows.get("hug.songCover"));
        }
        return out;
    }

    /** 保存抱抱安全歌：校验与素材绑定同源（local 回填曲库冗余 / gequhai 必须带外部 ID） */
    @Transactional
    public void saveHugConfig(String songSource, Long songId, String songExternalId,
                              String songTitle, String songArtist, String songCover) {
        if (songId == null && (songExternalId == null || songExternalId.isBlank())) {
            // 清除配置
            repoDeleteByPrefix("hug.");
            return;
        }
        boolean external = "gequhai".equals(songSource);
        upsertHug("hug.songSource", external ? "gequhai" : "local");
        upsertHug("hug.songId", external ? null : String.valueOf(songId));
        upsertHug("hug.songExternalId", external ? songExternalId.trim() : null);
        if (external) {
            upsertHug("hug.songTitle", songTitle);
            upsertHug("hug.songArtist", songArtist);
            upsertHug("hug.songCover", songCover);
        } else {
            Song song = songRepo.findById(songId).orElse(null);
            if (song == null) {
                throw new IllegalArgumentException("本地歌曲不存在");
            }
            upsertHug("hug.songTitle", song.getTitle());
            upsertHug("hug.songArtist", artistNames(song));
            upsertHug("hug.songCover", song.getCover());
        }
    }

    private Map<String, String> hugRows() {
        Map<String, String> m = new LinkedHashMap<>();
        systemConfigRepo.findByConfigKeyStartingWith("hug.")
                .forEach(c -> m.put(c.getConfigKey(), c.getConfigValue()));
        return m;
    }

    private void upsertHug(String key, String value) {
        if (value == null) {
            return;
        }
        SystemConfig c = systemConfigRepo.findByConfigKey(key).orElseGet(() -> {
            SystemConfig n = new SystemConfig();
            n.setConfigKey(key);
            return n;
        });
        c.setConfigValue(value);
        c.setUpdateTime(LocalDateTime.now());
        systemConfigRepo.save(c);
    }

    private void repoDeleteByPrefix(String prefix) {
        systemConfigRepo.deleteByConfigKeyStartingWith(prefix);
    }

    /** 抱抱冷却（单实例内存态即可：重启丢失最多多收一次通知，无伤大雅） */
    private final java.util.concurrent.ConcurrentHashMap<Long, Long> hugCooldown = new java.util.concurrent.ConcurrentHashMap<>();
    private static final long HUG_COOLDOWN_MS = 30_000L;

    /**
     * 抱抱：冷却校验 → 给管理员发通知；返回安全歌配置（前端决定放不放歌）
     */
    @Transactional
    public Map<String, Object> sendHug(Long userId) {
        long now = System.currentTimeMillis();
        Long last = hugCooldown.get(userId);
        if (last != null && now - last < HUG_COOLDOWN_MS) {
            throw new IllegalStateException("刚刚已经抱过啦，歇一会儿再来");
        }
        hugCooldown.put(userId, now);

        User hugger = userRepo.findById(userId).orElse(null);
        String nickname = hugger == null ? "她" : (hugger.getNickname() != null && !hugger.getNickname().isBlank() ? hugger.getNickname() : hugger.getUsername());
        for (User admin : userRepo.findAllByRole("admin")) {
            notificationService.createNotification(admin.getId(), userId, "hug",
                    "🫂 有一个抱抱请求",
                    nickname + " 刚刚按下了「需要抱抱」按钮，去看看吧",
                    null, "memory", "/memories");
        }
        return getHugConfig();
    }

    /**
     * 归一化歌曲绑定：gequhai 来源校验外部 ID（冗余展示字段沿用面板传值）；
     * local 来源回填曲库冗余字段（歌曲不存在/已删 → 清空绑定，前端隐藏 ♪）
     */
    private void normalizeSongFields(MemoryItem item) {
        if ("gequhai".equals(item.getSongSource())) {
            item.setSongId(null);
            if (item.getSongExternalId() == null || item.getSongExternalId().isBlank()) {
                item.setSongSource("local");
                clearSongRedundant(item);
            }
            return;
        }
        item.setSongSource("local");
        item.setSongExternalId(null);
        if (item.getSongId() == null) {
            clearSongRedundant(item);
            return;
        }
        Song song = songRepo.findById(item.getSongId()).orElse(null);
        if (song == null) {
            item.setSongId(null);
            clearSongRedundant(item);
            return;
        }
        item.setSongTitle(song.getTitle());
        item.setSongArtist(artistNames(song));
        item.setSongCover(song.getCover());
    }

    private void clearSongRedundant(MemoryItem item) {
        item.setSongTitle(null);
        item.setSongArtist(null);
        item.setSongCover(null);
    }

    private String artistNames(Song s) {
        if (s.getArtists() == null || s.getArtists().isEmpty()) {
            return "未知歌手";
        }
        return s.getArtists().stream().map(Artist::getName).reduce((a, b) -> a + " / " + b).orElse("未知歌手");
    }
}
