package com.example.sheepmusic.dto;

import com.example.sheepmusic.entity.Song;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 推荐结果条目（推荐系统 v2）
 * 携带歌曲实体 + 融合得分 + 推荐理由 + 召回策略标识
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RecommendItemVO {

    /** 推荐的歌曲 */
    private Song song;

    /** 融合得分（多通道加权归一后，仅用于排序展示） */
    private double score;

    /** 推荐理由（来自真实依据：共现种子歌名/风格/歌手名/榜单来源） */
    private String reason;

    /** 召回策略：cf-协同 / content-内容特征 / artist-歌手维度 / hot-热度 / fresh-新鲜度 */
    private String strategy;
}
