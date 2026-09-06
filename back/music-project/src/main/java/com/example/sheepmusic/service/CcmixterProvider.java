package com.example.sheepmusic.service;

import com.example.sheepmusic.dto.ExternalSearchResultVO;
import com.example.sheepmusic.dto.ExternalSongVO;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * ccMixter 创作社区（https://ccmixter.org，dig API，无需任何密钥）
 * 全站 CC 授权的原创/混音音乐；内容路径按 UA/Referer 做防护，
 * 流式代理需携带浏览器 UA + Referer（见 streamHeaders）
 */
@Component
public class CcmixterProvider implements MusicSourceProvider {

    private static final String API_BASE = "https://ccmixter.org/api/query";
    private static final ObjectMapper objectMapper = new ObjectMapper();
    private static final String BROWSER_UA =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/126.0 Safari/537.36";

    private final RestClient restClient;

    public CcmixterProvider() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(5_000);
        // ccMixter 上游响应偏慢（实测 ~3s），放宽读取超时
        factory.setReadTimeout(15_000);
        this.restClient = RestClient.builder()
                .requestFactory(factory)
                .defaultHeader("User-Agent", BROWSER_UA)
                .build();
    }

    @Override
    public String source() {
        return "ccmixter";
    }

    @Override
    public String label() {
        return "ccMixter 创作社区";
    }

    @Override
    public boolean isEnabled() {
        return true;
    }

    @Override
    public ExternalSearchResultVO search(String keyword, int page, int size) throws Exception {
        URI uri = UriComponentsBuilder.fromHttpUrl(API_BASE)
                .queryParam("f", "json")
                .queryParam("any", keyword)
                .queryParam("limit", size)
                .queryParam("offset", page * (long) size)
                .build()
                .encode()
                .toUri();

        // ccMixter 返回 Content-Type: text/plain，RestClient 无法按类型直反序列化，取 String 自行解析
        String body = restClient.get().uri(uri).retrieve().body(String.class);
        JsonNode items = body == null || body.isBlank() ? null : objectMapper.readTree(body);
        List<ExternalSongVO> songs = new ArrayList<>();
        if (items != null && items.isArray()) {
            for (JsonNode item : items) {
                ExternalSongVO vo = toSong(item);
                if (vo != null) {
                    songs.add(vo);
                }
            }
        }
        // dig API 不返回总数，total=-1 表示未知，前端不展示分页
        return new ExternalSearchResultVO(source(), label(), true, null, -1, songs);
    }

    @Override
    public String resolveStreamUrl(String trackId, String fileHint) {
        // fileHint 为搜索时从 download_url 提取的相对内容路径（用户名/文件名.mp3），
        // 由 Controller 做安全校验（安全字符集、禁止 ..）后透传
        return "https://ccmixter.org/content/" + fileHint;
    }

    @Override
    public Map<String, String> streamHeaders() {
        return Map.of("User-Agent", BROWSER_UA, "Referer", "https://ccmixter.org/");
    }

    private ExternalSongVO toSong(JsonNode item) {
        JsonNode files = item.path("files");
        if (!files.isArray() || files.isEmpty()) {
            return null;
        }
        JsonNode best = null;
        for (JsonNode f : files) {
            String mime = f.path("file_format_info").path("mime_type").asText("");
            if (!mime.startsWith("audio/")) {
                continue;
            }
            // 优先标准 mp3
            if ("mp3".equals(f.path("file_nicname").asText())) {
                best = f;
                break;
            }
            if (best == null) {
                best = f;
            }
        }
        if (best == null) {
            return null;
        }

        ExternalSongVO vo = new ExternalSongVO();
        vo.setSourceTrackId(item.path("upload_id").asText());
        vo.setTitle(item.path("upload_name").asText());
        String realName = item.path("user_real_name").asText("");
        vo.setArtist(realName.isBlank() ? item.path("user_name").asText() : realName);
        vo.setCover(null);
        vo.setDuration(parseDuration(best.path("file_format_info").path("ps").asText(null)));
        // fileHint = download_url 中 /content/ 之后的相对路径（用户名/文件名），代理端据此拼回直链
        String downloadUrl = best.path("download_url").asText(null);
        String fileHint = contentPath(downloadUrl);
        if (fileHint == null) {
            return null;
        }
        vo.setStreamUrl("/api/music/external/stream?source=" + source()
                + "&trackId=" + vo.getSourceTrackId()
                + "&fileId=" + fileHint);
        vo.setDownloadUrl(downloadUrl);
        vo.setLicenseUrl(item.path("license_url").asText(null));
        vo.setLicenseName(MusicSourceProvider.ccLicenseName(vo.getLicenseUrl()));
        return vo;
    }

    /** https://ccmixter.org/content/{user}/{file}.mp3 → {user}/{file}.mp3 */
    private String contentPath(String downloadUrl) {
        if (downloadUrl == null) {
            return null;
        }
        int idx = downloadUrl.indexOf("/content/");
        if (idx < 0) {
            return null;
        }
        String path = downloadUrl.substring(idx + "/content/".length());
        return path.isBlank() ? null : path;
    }

    /** "3:04" / "1:02:03" → 秒 */
    private Integer parseDuration(String ps) {
        if (ps == null || ps.isBlank()) {
            return null;
        }
        try {
            String[] parts = ps.split(":");
            int seconds = 0;
            for (String p : parts) {
                seconds = seconds * 60 + Integer.parseInt(p.trim());
            }
            return seconds;
        } catch (Exception e) {
            return null;
        }
    }
}
