package com.example.sheepmusic.service;

import com.example.sheepmusic.dto.ExternalSearchResultVO;
import com.example.sheepmusic.dto.ExternalSongVO;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

/**
 * Jamendo 开放曲库（CC 授权音乐，https://devportal.jamendo.com）
 * client_id 在 Jamendo 开发者后台免费注册；未配置时 isEnabled=false，功能降级
 */
@Component
public class JamendoProvider implements MusicSourceProvider {

    private static final String STREAM_PATH = "/?trackid=%s&format=mp31";

    @Value("${external.sources.jamendo.client-id:}")
    private String clientId;

    @Value("${external.sources.jamendo.api-base-url:https://api.jamendo.com/v3.0}")
    private String apiBaseUrl;

    @Value("${external.sources.jamendo.stream-base-url:https://prod-1.storage.jamendo.com}")
    private String streamBaseUrl;

    private final RestClient restClient;

    public JamendoProvider() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(5_000);
        factory.setReadTimeout(8_000);
        this.restClient = RestClient.builder().requestFactory(factory).build();
    }

    @Override
    public String source() {
        return "jamendo";
    }

    @Override
    public String label() {
        return "Jamendo 开放曲库";
    }

    @Override
    public boolean isEnabled() {
        return clientId != null && !clientId.isBlank();
    }

    @Override
    public ExternalSearchResultVO search(String keyword, int page, int size) throws Exception {
        URI uri = UriComponentsBuilder.fromHttpUrl(apiBaseUrl + "/tracks/")
                .queryParam("client_id", clientId)
                .queryParam("format", "json")
                .queryParam("search", keyword)
                .queryParam("limit", size)
                .queryParam("offset", page * (long) size)
                .build()
                .encode()
                .toUri();

        JsonNode root = restClient.get().uri(uri).retrieve().body(JsonNode.class);
        JsonNode headers = root == null ? null : root.path("headers");
        if (headers == null || !"success".equals(headers.path("status").asText())) {
            throw new IllegalStateException("Jamendo 返回异常状态: "
                    + (headers == null ? "空响应" : headers.path("error_message").asText("未知")));
        }

        List<ExternalSongVO> items = new ArrayList<>();
        for (JsonNode track : root.path("results")) {
            ExternalSongVO vo = new ExternalSongVO();
            vo.setSourceTrackId(track.path("id").asText());
            vo.setTitle(track.path("name").asText());
            vo.setArtist(track.path("artist_name").asText());
            vo.setAlbum(track.path("album_name").asText());
            vo.setCover(track.path("album_image").asText(null));
            vo.setDuration(track.path("duration").asInt(0));
            vo.setStreamUrl(streamProxyUrl(vo.getSourceTrackId()));
            // Jamendo 关闭下载授权时 audiodownload_allowed=false
            if (track.path("audiodownload_allowed").asBoolean(false)) {
                vo.setDownloadUrl(track.path("audiodownload").asText(null));
            }
            vo.setLicenseUrl(track.path("license_ccurl").asText(null));
            vo.setLicenseName(licenseName(vo.getLicenseUrl()));
            items.add(vo);
        }

        long total = headers.path("available").asLong(headers.path("results_count").asLong(-1));
        return new ExternalSearchResultVO(source(), label(), true, null, total, items);
    }

    @Override
    public String resolveStreamUrl(String trackId, String fileHint) {
        return streamBaseUrl + String.format(STREAM_PATH, trackId);
    }

    /** 前端播放地址：走后端同源流式代理（Jamendo CDN 无 CORS 头，直链会被 crossOrigin=anonymous 拒载） */
    private String streamProxyUrl(String trackId) {
        return "/api/music/external/stream?source=" + source() + "&trackId=" + trackId;
    }

    /** 授权简称统一在 SPI 静态方法实现（ccMixter 等源共用） */
    private String licenseName(String licenseUrl) {
        return MusicSourceProvider.ccLicenseName(licenseUrl);
    }
}
