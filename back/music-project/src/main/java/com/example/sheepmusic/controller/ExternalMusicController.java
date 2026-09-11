package com.example.sheepmusic.controller;

import com.example.sheepmusic.common.Result;
import com.example.sheepmusic.dto.ExternalSearchResultVO;
import com.example.sheepmusic.dto.ExternalSourceVO;
import com.example.sheepmusic.service.ExternalMusicService;
import com.example.sheepmusic.service.MusicSourceProvider;
import com.example.sheepmusic.service.MusicSourceRegistry;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Comparator;
import java.util.List;

/**
 * 外源开放曲库（曲库供应链 v1，见 docs/specs/曲库供应链v1/）
 * search 需登录（与本地搜索一致）；stream 免登录——<audio> 无法携带 JWT，
 * 暴露面由"数字 trackId 校验 + 音源白名单 + 仅代理音频"收敛
 */
@Slf4j
@Tag(name = "曲库供应链")
@RestController
@RequestMapping("/music/external")
@CrossOrigin
public class ExternalMusicController {

    @Autowired
    private ExternalMusicService externalMusicService;

    @Autowired
    private MusicSourceRegistry registry;

    /** 上游音频连接：连接 5s；流式读取不设整体超时，空闲由 JDK HttpClient 与容器管理 */
    private final HttpClient streamClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .followRedirects(HttpClient.Redirect.ALWAYS)
            .build();

    @Operation(summary = "音源列表（含启用状态，前端分区用）")
    @GetMapping("/sources")
    public Result<List<ExternalSourceVO>> sources() {
        List<ExternalSourceVO> list = registry.all().stream()
                // 启用的源排前面，同状态按标识排序，保证分区顺序稳定
                .sorted(Comparator.comparing((MusicSourceProvider p) -> !p.isEnabled())
                        .thenComparing(MusicSourceProvider::source))
                .map(p -> new ExternalSourceVO(p.source(), p.label(), p.isEnabled(), p.openLicensed()))
                .toList();
        return Result.success(list);
    }

    /** fileId 安全校验：安全字符集（字母/数字/点/下划线/连字符/空格），可含一层 "/"，禁止 ".." 等路径穿越 */
    private boolean isSafeFileId(String fileId) {
        if (fileId.length() > 200 || fileId.contains("..")) {
            return false;
        }
        return fileId.matches("[A-Za-z0-9._\\- ]{1,100}(?:/[A-Za-z0-9._\\- ]{1,150}){0,2}");
    }

    @Operation(summary = "外源歌词（LRC 文本，无歌词返回空串）")
    @GetMapping("/lyric")
    public Result<String> lyric(
            @Parameter(description = "音源标识", example = "gequhai")
            @RequestParam String source,
            @Parameter(description = "外部曲目 ID（数字）", example = "326")
            @RequestParam String trackId
    ) {
        MusicSourceProvider provider = registry.optionalGet(source).orElse(null);
        if (provider == null || trackId == null || !trackId.matches("\\d{1,20}")) {
            return Result.error(400, "参数错误");
        }
        try {
            return Result.success(provider.resolveLyric(trackId));
        } catch (Exception e) {
            log.warn("外源歌词获取失败 [{}#{}]: {}", source, trackId, e.getMessage());
            return Result.success("");
        }
    }

    @Operation(summary = "外源歌曲封面 URL（无则返回空串）")
    @GetMapping("/cover")
    public Result<String> cover(
            @Parameter(description = "音源标识", example = "gequhai")
            @RequestParam String source,
            @Parameter(description = "外部曲目 ID（数字）", example = "326")
            @RequestParam String trackId
    ) {
        MusicSourceProvider provider = registry.optionalGet(source).orElse(null);
        if (provider == null || trackId == null || !trackId.matches("\\d{1,20}")) {
            return Result.error(400, "参数错误");
        }
        try {
            return Result.success(provider.resolveCover(trackId));
        } catch (Exception e) {
            log.warn("外源封面获取失败 [{}#{}]: {}", source, trackId, e.getMessage());
            return Result.success("");
        }
    }

    @Operation(summary = "搜索外源开放曲库歌曲")
    @GetMapping("/search")
    public Result<ExternalSearchResultVO> search(
            @Parameter(description = "音源标识", example = "gequhai")
            @RequestParam(defaultValue = "jamendo") String source,
            @Parameter(description = "搜索关键词") @RequestParam(required = false) String keyword,
            @Parameter(description = "页码（0 起）", example = "0")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "每页数量（≤50）", example = "20")
            @RequestParam(defaultValue = "20") int size
    ) {
        try {
            return Result.success(externalMusicService.search(source, keyword, page, size));
        } catch (IllegalArgumentException e) {
            return Result.error(400, e.getMessage());
        } catch (Exception e) {
            log.error("开放曲库搜索接口异常", e);
            return Result.error("搜索开放曲库失败，请稍后重试");
        }
    }

    @Operation(summary = "外源音频流式代理（免登录，支持 Range）")
    @GetMapping("/stream")
    public ResponseEntity<InputStreamResource> stream(
            @Parameter(description = "音源标识", example = "gequhai")
            @RequestParam String source,
            @Parameter(description = "外部曲目 ID", example = "1442761")
            @RequestParam String trackId,
            @Parameter(description = "文件标识（数字 ID 或 音源内容相对路径）", example = "61fd04c6ec4b")
            @RequestParam(required = false) String fileId,
            @RequestHeader(value = HttpHeaders.RANGE, required = false) String range
    ) {
        MusicSourceProvider provider = registry.optionalGet(source).orElse(null);
        if (provider == null) {
            return ResponseEntity.badRequest().build();
        }
        // 防开放代理：trackId 必须是纯数字；fileId 可缺省，提供时限定安全字符集且禁止 ".."（ccMixter 为 用户名/文件名 路径），
        // 上游 URL 只能由 Provider 内置白名单前缀 + 校验过的标识拼出
        if (trackId == null || !trackId.matches("\\d{1,20}")
                || (fileId != null && !isSafeFileId(fileId))) {
            return ResponseEntity.badRequest().build();
        }

        String upstreamUrl;
        try {
            upstreamUrl = provider.resolveStreamUrl(trackId, fileId);
        } catch (Exception e) {
            log.warn("外源音频地址解析失败 [{}#{}]: {}", source, trackId, e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY).build();
        }
        HttpRequest.Builder reqBuilder = HttpRequest.newBuilder(URI.create(upstreamUrl))
                .timeout(Duration.ofSeconds(30))
                .header(HttpHeaders.USER_AGENT, "SheepMusic/1.0 (+stream-proxy)");
        if (range != null && !range.isBlank()) {
            reqBuilder.header(HttpHeaders.RANGE, range);
        }

        HttpResponse<java.io.InputStream> upstream;
        try {
            upstream = streamClient.send(reqBuilder.GET().build(), HttpResponse.BodyHandlers.ofInputStream());
        } catch (Exception e) {
            log.warn("外源音频上游请求失败 [{}#{}]: {}", source, trackId, e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY).build();
        }

        int code = upstream.statusCode();
        if (code != 200 && code != 206) {
            try {
                upstream.body().close();
            } catch (Exception ignored) {
            }
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY).build();
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(upstream.headers().firstValue(HttpHeaders.CONTENT_TYPE)
                .map(MediaType::parseMediaType)
                .orElse(MediaType.APPLICATION_OCTET_STREAM));
        headers.set(HttpHeaders.ACCEPT_RANGES, "bytes");
        String contentRange = upstream.headers().firstValue(HttpHeaders.CONTENT_RANGE).orElse(null);
        if (contentRange != null) {
            headers.set(HttpHeaders.CONTENT_RANGE, contentRange);
        }
        long contentLength = upstream.headers().firstValueAsLong(HttpHeaders.CONTENT_LENGTH).orElse(-1);

        ResponseEntity.BodyBuilder resp = ResponseEntity.status(code).headers(headers);
        if (contentLength >= 0) {
            resp.contentLength(contentLength);
        }
        return resp.body(new InputStreamResource(upstream.body()));
    }
}
