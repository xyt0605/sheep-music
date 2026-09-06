package com.example.sheepmusic.service;

import com.example.sheepmusic.dto.ExternalSearchResultVO;

/**
 * 外源音源 SPI（曲库供应链 v1）
 * 新增音源 = 新增一个实现 + 配置项，搜索页分区自动扩展，无需改动既有代码
 */
public interface MusicSourceProvider {

    /** 音源标识（URL 参数用，小写，如 jamendo） */
    String source();

    /** 音源展示名 */
    String label();

    /** 是否已启用（密钥等配置齐全） */
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
     */
    String resolveStreamUrl(String trackId);
}
