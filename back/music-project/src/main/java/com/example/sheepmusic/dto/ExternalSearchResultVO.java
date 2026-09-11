package com.example.sheepmusic.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 外源开放曲库搜索结果（曲库供应链 v1）
 * enabled=false 表示音源未配置（如缺 client_id），message 携带配置指引；
 * enabled=true 但上游失败时 items 为空、message 说明原因（降级不报 500）
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ExternalSearchResultVO {

    /** 音源标识（jamendo 等） */
    private String source;

    /** 音源展示名（Jamendo 开放曲库） */
    private String sourceLabel;

    /** 音源是否已启用（密钥等配置齐全） */
    private boolean enabled;

    /** 提示信息：配置指引 / 上游异常说明，可为空 */
    private String message;

    /** 命中总数（用于分页，上游不可得时为 -1） */
    private long total;

    /** 结果列表 */
    private List<ExternalSongVO> items;
}
