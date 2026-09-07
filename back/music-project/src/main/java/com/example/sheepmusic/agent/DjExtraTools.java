package com.example.sheepmusic.agent;

import com.example.sheepmusic.entity.Playlist;
import com.example.sheepmusic.entity.PlaylistSong;
import com.example.sheepmusic.entity.Song;
import com.example.sheepmusic.service.ExternalMusicService;
import com.example.sheepmusic.service.GequhaiProvider;
import com.example.sheepmusic.service.PlaylistService;
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
import java.util.Set;

/**
 * 小屋 DJ 能力扩展工具集（agent v1 第一批：播放控制 / 知识问答 / 歌单生成）
 * 播放控制本身不在这里执行——编排器把命令透传为 player_command 事件，由前端播放器 store 执行
 */
@Slf4j
@Service
public class DjExtraTools {

    /** 播放器支持的命令集（前端 player store 执行；play_ref 需携带歌曲卡片数据） */
    public static final Set<String> PLAYER_COMMANDS = Set.of(
            "play", "pause", "next", "prev", "volume_up", "volume_down",
            "mode_list", "mode_random", "mode_single", "queue_clear", "play_ref");

    @Autowired
    private SongService songService;

    @Autowired
    private PlaylistService playlistService;

    @Autowired
    private ExternalMusicService externalMusicService;

    @Autowired
    private GequhaiProvider gequhaiProvider;

    /** 校验播放命令是否合法 */
    public boolean isValidPlayerCommand(String command) {
        return command != null && PLAYER_COMMANDS.contains(command);
    }

    /** search_local 同款检索（供 play_ref 找歌）：返回带 ref 的候选（含本地/歌曲海双源，本地优先） */
    public Map<String, Object> findSongForPlay(String keyword) {
        Map<String, Object> out = new LinkedHashMap<>();
        List<Map<String, Object>> candidates = new ArrayList<>();

        // 本地曲库优先
        Pageable pageable = PageRequest.of(0, 3);
        for (Song s : songService.searchSongs(keyword, pageable).getContent()) {
            if (s.getStatus() == null || s.getStatus() != 1) {
                continue;
            }
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("type", "local");
            item.put("songId", s.getId());
            item.put("title", s.getTitle());
            item.put("artist", s.getArtists() == null || s.getArtists().isEmpty() ? "" : s.getArtists().get(0).getName());
            item.put("cover", s.getCover());
            item.put("url", s.getUrl());
            candidates.add(item);
        }
        // 本地不足时补歌曲海（试听）
        if (candidates.size() < 2) {
            try {
                var result = externalMusicService.search("gequhai", keyword, 0, 3);
                for (var vo : result.getItems()) {
                    if (candidates.size() >= 3) {
                        break;
                    }
                    Map<String, Object> item = new LinkedHashMap<>();
                    item.put("type", "gequhai");
                    item.put("sourceTrackId", vo.getSourceTrackId());
                    item.put("title", vo.getTitle());
                    item.put("artist", vo.getArtist());
                    item.put("cover", vo.getCover());
                    item.put("streamUrl", vo.getStreamUrl());
                    item.put("isExternal", true);
                    candidates.add(item);
                }
            } catch (Exception e) {
                log.debug("播放找歌补充歌曲海失败: {}", e.getMessage());
            }
        }
        out.put("candidates", candidates);
        return out;
    }

    /** song_info：歌曲海曲目详情（歌词全文 + 元数据）。仅支持歌曲海条目 */
    public Map<String, Object> songInfo(String source, String trackId, String title) {
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("title", title == null ? "" : title);
        if (!"gequhai".equals(source)) {
            out.put("supported", false);
            out.put("note", "本地曲库歌曲暂无详情页，可尝试用歌词内容搜索");
            return out;
        }
        try {
            String lyric = gequhaiProvider.resolveLyric(trackId);
            out.put("supported", true);
            out.put("lyric", lyric == null ? "" : lyric);
            // LRC 头部元信息（[ti:标题][ar:歌手][al:专辑][by:]）解析
            for (String line : (lyric == null ? "" : lyric).split("\n")) {
                if (line.length() > 40 || !line.startsWith("[")) {
                    continue;
                }
                if (line.matches("(?i)^\\[ti:.*\\]\\s*$") && line.length() > 5) {
                    out.put("metaTitle", line.replaceAll("\\[ti:|\\]", "").trim());
                } else if (line.matches("(?i)^\\[ar:.*\\]\\s*$")) {
                    out.put("metaArtist", line.replaceAll("\\[ar:|\\]", "").trim());
                } else if (line.matches("(?i)^\\[al:.*\\]\\s*$")) {
                    out.put("metaAlbum", line.replaceAll("\\[al:|\\]", "").trim());
                }
            }
        } catch (Exception e) {
            out.put("supported", false);
            out.put("note", "详情获取失败：" + e.getMessage());
        }
        return out;
    }

    /** lyric_search：按歌词内文在歌曲海搜歌（用户问"歌词里有XX的是什么歌"） */
    public List<Map<String, Object>> lyricSearch(String lyricLine) {
        List<Map<String, Object>> out = new ArrayList<>();
        try {
            var result = externalMusicService.search("gequhai", lyricLine, 0, 5);
            for (var vo : result.getItems()) {
                Map<String, Object> item = new HashMap<>();
                item.put("type", "gequhai");
                item.put("sourceTrackId", vo.getSourceTrackId());
                item.put("title", vo.getTitle());
                item.put("artist", vo.getArtist());
                item.put("cover", vo.getCover());
                item.put("streamUrl", vo.getStreamUrl());
                item.put("isExternal", true);
                out.add(item);
            }
        } catch (Exception e) {
            log.debug("歌词搜歌失败: {}", e.getMessage());
        }
        return out;
    }

    /** create_playlist：把候选存为用户本地歌单。仅本地 songId 可入库；返回统计与歌单信息 */
    public Map<String, Object> createPlaylist(Long userId, String name, List<Map<String, Object>> picked) {
        Map<String, Object> out = new LinkedHashMap<>();
        List<Long> songIds = new ArrayList<>();
        int skipped = 0;
        for (Map<String, Object> item : picked) {
            if (Boolean.TRUE.equals(item.get("isExternal")) || item.get("songId") == null) {
                skipped++;
                continue;
            }
            songIds.add(((Number) item.get("songId")).longValue());
        }
        if (songIds.isEmpty()) {
            out.put("ok", false);
            out.put("message", "候选里没有可入库的本地歌曲（歌曲海为试听源不入库）");
            return out;
        }
        try {
            Playlist pl = new Playlist();
            pl.setName(name == null || name.isBlank() ? "小羊驼精选" : name.trim());
            pl.setUserId(userId);
            pl.setIsPublic(false);
            pl.setDescription("小羊驼为你生成");
            Playlist created = playlistService.createPlaylist(pl);
            playlistService.addSongsToPlaylist(created.getId(), songIds, userId);
            out.put("ok", true);
            out.put("playlistId", created.getId());
            out.put("name", created.getName());
            out.put("count", songIds.size());
            out.put("skipped", skipped);
        } catch (Exception e) {
            log.warn("DJ 创建歌单失败: {}", e.getMessage());
            out.put("ok", false);
            out.put("message", "创建失败：" + e.getMessage());
        }
        return out;
    }
}
