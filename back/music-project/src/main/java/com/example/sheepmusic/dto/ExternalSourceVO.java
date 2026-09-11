package com.example.sheepmusic.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 外源音源信息（/music/external/sources 返回，前端据此生成搜索分区）
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ExternalSourceVO {

    /** 音源标识 */
    private String source;

    /** 展示名 */
    private String label;

    /** 是否已启用（未启用的源仅展示配置指引，不参与搜索） */
    private boolean enabled;

    /** 是否开放授权曲库（CC 等；false 表示聚合试听源） */
    private boolean openLicense;
}
