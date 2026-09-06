package com.example.sheepmusic.service;

import com.example.sheepmusic.dto.ExternalSearchResultVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * 外源音源编排（曲库供应链 v1）
 * 校验参数 → 查找音源 → 委托搜索；上游异常统一降级为空结果，不冒泡 500
 */
@Slf4j
@Service
public class ExternalMusicService {

    private static final int MAX_PAGE_SIZE = 50;

    @Autowired
    private MusicSourceRegistry registry;

    public ExternalSearchResultVO search(String source, String keyword, int page, int size) {
        if (!StringUtils.hasText(keyword)) {
            throw new IllegalArgumentException("搜索关键词不能为空");
        }
        MusicSourceProvider provider = registry.optionalGet(source)
                .orElseThrow(() -> new IllegalArgumentException("不支持的音乐来源: " + source));
        if (keyword.length() > 64) {
            throw new IllegalArgumentException("搜索关键词过长");
        }
        int safePage = Math.max(0, page);
        int safeSize = Math.min(Math.max(1, size), MAX_PAGE_SIZE);

        if (!provider.isEnabled()) {
            return new ExternalSearchResultVO(provider.source(), provider.label(), false,
                    "音源未启用：请配置对应的环境变量后重启（Jamendo 需 JAMENDO_CLIENT_ID，"
                            + "在 devportal.jamendo.com 免费注册）", 0, java.util.List.of());
        }

        try {
            return provider.search(keyword.trim(), safePage, safeSize);
        } catch (Exception e) {
            log.warn("外源音源 [{}] 搜索失败，降级为空结果: {}", source, e.getMessage());
            return new ExternalSearchResultVO(provider.source(), provider.label(), true,
                    "开放曲库暂时不可用，请稍后再试", -1, java.util.List.of());
        }
    }
}
