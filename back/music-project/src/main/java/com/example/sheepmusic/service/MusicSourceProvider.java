package com.example.sheepmusic.service;

import com.example.sheepmusic.dto.ExternalSearchResultVO;

import java.net.URI;
import java.util.Map;

/**
 * 外源音源 SPI（曲库供应链 v1）
 * 新增音源 = 新增一个实现 + 配置项，搜索页分区自动扩展，无需改动既有代码
 */
public interface MusicSourceProvider {

    /** 音源标识（URL 参数用，小写，如 jamendo / ccmixter） */
    String source();

    /** 音源展示名 */
    String label();

    /** 是否已启用（密钥等配置齐全；无配置要求的源恒为 true） */
    boolean isEnabled();

    /**
     * 关键词搜索（page 从 0 起）
     *
     * @throws Exception 上游调用/解析失败——由 ExternalMusicService 统一降级，实现内不要吞异常
     */
    ExternalSearchResultVO search(String keyword, int page, int size) throws Exception;

    /**
     * 解析曲目真实音频地址（流式代理上游用）
     *
     * @param trackId 已由调用方做过格式校验
     * @param fileHint 同一条目含多个音频文件时的文件标识（如 ccMixter 的 file_id），可为 null
     * @throws Exception 解析失败——流式代理端统一转 502
     */
    String resolveStreamUrl(String trackId, String fileHint) throws Exception;

    /**
     * 流式代理访问该音源上游时附加的请求头（部分站点按 UA/Referer 防护，如 ccMixter）。
     * 返回空 Map 表示使用代理默认头。
     */
    default Map<String, String> streamHeaders() {
        return Map.of();
    }

    /** 是否开放授权曲库（决定搜索分区展示"CC 授权"还是"聚合试听"标识） */
    default boolean openLicensed() {
        return false;
    }

    /**
     * 解析歌词（LRC 文本；无歌词返回空串）。默认不支持。
     */
    default String resolveLyric(String trackId) {
        return "";
    }

    /**
     * 从 Creative Commons 授权链接推导简称，
     * 如 https://creativecommons.org/licenses/by-nc-nd/3.0/ → "CC BY-NC-ND 3.0"
     */
    static String ccLicenseName(String licenseUrl) {
        if (licenseUrl == null || licenseUrl.isBlank()) {
            return "CC";
        }
        try {
            String path = URI.create(licenseUrl).getPath();
            if (!path.contains("/licenses/")) {
                return "CC";
            }
            String after = path.substring(path.indexOf("/licenses/") + "/licenses/".length());
            String[] parts = after.split("/");
            String code = parts.length > 0 ? parts[0].toUpperCase().replace("-", " ") : "";
            String version = parts.length > 1 ? " " + parts[1] : "";
            return code.isBlank() ? "CC" : "CC " + code + version;
        } catch (Exception e) {
            return "CC";
        }
    }
}
