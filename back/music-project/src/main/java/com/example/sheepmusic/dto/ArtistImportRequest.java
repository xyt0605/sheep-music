package com.example.sheepmusic.dto;

import lombok.Data;

import java.util.List;

@Data
public class ArtistImportRequest {
    private List<String> names;
}
