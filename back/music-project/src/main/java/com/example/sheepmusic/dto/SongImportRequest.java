package com.example.sheepmusic.dto;

import lombok.Data;

import jakarta.validation.constraints.NotBlank;
import java.util.List;

@Data
public class SongImportRequest {
    @NotBlank(message = "歌曲名称不能为空")
    private String title;

    private List<Long> artistIds;
    private List<String> artistNames;
    private Long albumId;
    private String albumName;
    private String genre;
    private String language;
    private Integer duration;
    private String cover;

    @NotBlank(message = "音频文件URL不能为空")
    private String url;

    private String lyric;
    private Integer status;
}
