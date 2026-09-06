package com.example.sheepmusic.service;

import com.example.sheepmusic.dto.ExternalSearchResultVO;
import com.example.sheepmusic.dto.ExternalSongVO;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.net.CookieManager;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 歌曲海聚合源（曲库供应链 v1.2，用户指定接入）
 * 定性：聚合试听层——不入库、不进合规供应链、仅个人使用（授权状态不明，见交接文档 §16/§17）
 *
 * 链路（2026-09-06 实测）：搜索页 /s/{kw} HTML 解析 → /play/{id} 页内嵌会话绑定 token(play_id)
 * → POST /api/music（X-Requested-With: Http + X-Custom-Header: Key + 同 PHPSESSID）
 * → 酷我 CDN 直链；歌词在 /play 页 #content-lrc2（LRC，<br> 分隔）。
 * 注意：play_id 与会话绑定且直链带时间戳签名，解析结果短 TTL 缓存（5 分钟）。
 */
@Slf4j
@Component
public class GequhaiProvider implements MusicSourceProvider {

    private static final String BROWSER_UA =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/126.0 Safari/537.36";
    private static final Pattern PLAY_ID = Pattern.compile("window\\.play_id = '([0-9a-f]{32})'");
    private static final Pattern MP3_TYPE = Pattern.compile("window\\.mp3_type = (\\d+)");
    private static final Pattern MP3_COVER = Pattern.compile("window\\.mp3_cover = '([^']*)'");
    private static final long CACHE_TTL_MS = 5 * 60 * 1000L;
    private static final int CACHE_MAX = 300;

    @Value("${external.sources.gequhai.base-url:https://www.gequhai.com}")
    private String baseUrl;

    @Value("${external.sources.gequhai.enabled:true}")
    private boolean enabled;

    private final ObjectMapper objectMapper = new ObjectMapper();

    /** 会话保持：play_id 绑定 PHPSESSID，页面抓取与 /api/music 必须同一 Cookie 上下文 */
    private final HttpClient http = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(8))
            .followRedirects(HttpClient.Redirect.ALWAYS)
            .cookieHandler(new CookieManager())
            .build();

    /** /play 页解析缓存（play_id/type/歌词），流式播放与歌词共用一次抓取 */
    private final Map<String, PlayPage> pageCache = new ConcurrentHashMap<>();
    /** 解析出的音频直链缓存 */
    private final Map<String, CachedUrl> streamCache = new ConcurrentHashMap<>();

    private record PlayPage(String playId, int mp3Type, String lyric, String cover, long fetchedAt) {
    }

    private record CachedUrl(String url, long cachedAt) {
    }

    @Override
    public String source() {
        return "gequhai";
    }

    @Override
    public String label() {
        return "歌曲海聚合";
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    @Override
    public ExternalSearchResultVO search(String keyword, int page, int size) throws Exception {
        // /s/{kw} 分页从 1 起；站点每页固定条数，size 参数仅用于截断
        StringBuilder path = new StringBuilder("/s/").append(urlEncode(keyword));
        if (page > 0) {
            path.append("?page=").append(page + 1);
        }
        String html = httpGet(URI.create(baseUrl + path), Map.of());
        Document doc = Jsoup.parse(html, baseUrl);

        List<ExternalSongVO> songs = new ArrayList<>();
        for (Element row : doc.select("tr:has(a[href^=/play/])")) {
            Element link = row.selectFirst("a[href^=/play/]");
            if (link == null) {
                continue;
            }
            String id = link.attr("href").substring("/play/".length());
            if (!id.matches("\\d{1,12}")) {
                continue;
            }
            ExternalSongVO vo = new ExternalSongVO();
            vo.setSourceTrackId(id);
            vo.setTitle(link.text().trim());
            Element artistCell = row.select("td").size() > 2 ? row.select("td").get(2) : null;
            vo.setArtist(artistCell != null ? artistCell.text().trim() : "");
            vo.setDuration(null);
            vo.setStreamUrl("/api/music/external/stream?source=" + source() + "&trackId=" + id);
            songs.add(vo);
            if (songs.size() >= Math.min(Math.max(size, 1), 50)) {
                break;
            }
        }
        // HTML 搜索无总数信息
        return new ExternalSearchResultVO(source(), label(), true, null, -1, songs);
    }

    @Override
    public String resolveStreamUrl(String trackId, String fileHint) throws Exception {
        CachedUrl cached = streamCache.get(trackId);
        if (cached != null && System.currentTimeMillis() - cached.cachedAt() < CACHE_TTL_MS) {
            return cached.url();
        }
        PlayPage page = fetchPlayPage(trackId);

        // 表单：id=play_id&type=mp3_type，必需两个自定义头 + 会话 Cookie
        HttpRequest request = HttpRequest.newBuilder(URI.create(baseUrl + "/api/music"))
                .timeout(Duration.ofSeconds(10))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .header("X-Requested-With", "Http")
                .header("X-Custom-Header", "Key")
                .header("Referer", baseUrl + "/play/" + trackId)
                .header("User-Agent", BROWSER_UA)
                .POST(HttpRequest.BodyPublishers.ofString("id=" + page.playId() + "&type=" + page.mp3Type()))
                .build();
        HttpResponse<String> resp = http.send(request, HttpResponse.BodyHandlers.ofString());
        JsonNode node = objectMapper.readTree(resp.body());
        if (node.path("code").asInt() != 200) {
            throw new IllegalStateException("api/music 返回 code=" + node.path("code").asInt());
        }
        String url = node.path("data").path("url").asText("");
        if (url.isBlank()) {
            throw new IllegalStateException("api/music 未返回音频地址（可能是网盘专属曲目）");
        }
        // 部分曲目返回 antiserver 中转地址，需按 play.js 逻辑换取真实直链
        if (url.contains("antiserver.kuwo.cn")) {
            url = convertAntiserver(url);
        }

        if (streamCache.size() > CACHE_MAX) {
            streamCache.clear();
        }
        streamCache.put(trackId, new CachedUrl(url, System.currentTimeMillis()));
        return url;
    }

    @Override
    public String resolveLyric(String trackId) {
        try {
            return fetchPlayPage(trackId).lyric();
        } catch (Exception e) {
            log.warn("gequhai 歌词获取失败 [{}]: {}", trackId, e.getMessage());
            return "";
        }
    }

    @Override
    public String resolveCover(String trackId) {
        try {
            return fetchPlayPage(trackId).cover();
        } catch (Exception e) {
            log.warn("gequhai 封面获取失败 [{}]: {}", trackId, e.getMessage());
            return "";
        }
    }

    /** /play 页抓取+解析（缓存 5 分钟）；play_id 绑定会话，歌词/播放地址共用 */
    private PlayPage fetchPlayPage(String trackId) throws Exception {
        PlayPage cached = pageCache.get(trackId);
        if (cached != null && System.currentTimeMillis() - cached.fetchedAt() < CACHE_TTL_MS) {
            return cached;
        }
        String html = httpGet(URI.create(baseUrl + "/play/" + trackId), Map.of("Referer", baseUrl + "/"));

        Matcher idMatcher = PLAY_ID.matcher(html);
        Matcher typeMatcher = MP3_TYPE.matcher(html);
        if (!idMatcher.find()) {
            throw new IllegalStateException("播放页未包含 play_id（页面结构可能已变化）");
        }
        String playId = idMatcher.group(1);
        int mp3Type = typeMatcher.find() ? Integer.parseInt(typeMatcher.group(1)) : 0;
        String lyric = extractLyric(html);
        Matcher coverMatcher = MP3_COVER.matcher(html);
        String cover = coverMatcher.find() ? coverMatcher.group(1) : "";

        PlayPage page = new PlayPage(playId, mp3Type, lyric, cover, System.currentTimeMillis());
        if (pageCache.size() > CACHE_MAX) {
            pageCache.clear();
        }
        pageCache.put(trackId, page);
        return page;
    }

    /** antiserver.kuwo.cn 中转地址 → 真实直链（play.js 的 convert_url3 逻辑） */
    private String convertAntiserver(String antiserverUrl) throws Exception {
        URI uri = URI.create(antiserverUrl + (antiserverUrl.contains("?") ? "&" : "?") + "type=convert_url3");
        HttpResponse<String> resp = http.send(HttpRequest.newBuilder(uri)
                .timeout(Duration.ofSeconds(10))
                .header("User-Agent", BROWSER_UA)
                .GET().build(), HttpResponse.BodyHandlers.ofString());
        String body = resp.body().trim();
        // 可能是 JSONP 包裹（callback({...})）或纯 JSON
        if (body.startsWith("(") || body.contains("(")) {
            int l = body.indexOf('('), r = body.lastIndexOf(')');
            if (l >= 0 && r > l) {
                body = body.substring(l + 1, r);
            }
        }
        JsonNode node = objectMapper.readTree(body);
        if (node.path("code").asInt() == 200 && !node.path("url").asText("").isBlank()) {
            return node.path("url").asText();
        }
        throw new IllegalStateException("antiserver 转换失败 code=" + node.path("code").asInt());
    }

    /** #content-lrc2 节点 → 纯文本 LRC（<br> 转换行、去其余标签、解码实体） */
    private String extractLyric(String html) {
        try {
            Document doc = Jsoup.parse(html);
            Element node = doc.selectFirst("#content-lrc2");
            if (node == null) {
                return "";
            }
            String inner = node.html().replaceAll("(?i)<br\\s*/?>", "\n");
            return Jsoup.parse(inner).wholeText().trim();
        } catch (Exception e) {
            return "";
        }
    }

    private String httpGet(URI uri, Map<String, String> headers) throws Exception {
        HttpRequest.Builder builder = HttpRequest.newBuilder(uri)
                .timeout(Duration.ofSeconds(12))
                .header("User-Agent", BROWSER_UA)
                .GET();
        headers.forEach(builder::header);
        HttpResponse<String> resp = http.send(builder.build(), HttpResponse.BodyHandlers.ofString());
        if (resp.statusCode() != 200) {
            throw new IllegalStateException("上游 " + uri.getPath() + " 返回 " + resp.statusCode());
        }
        return resp.body();
    }

    private String urlEncode(String keyword) throws Exception {
        return java.net.URLEncoder.encode(keyword, java.nio.charset.StandardCharsets.UTF_8);
    }
}
