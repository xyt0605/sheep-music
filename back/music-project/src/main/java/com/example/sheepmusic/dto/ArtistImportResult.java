package com.example.sheepmusic.dto;

import com.example.sheepmusic.entity.Artist;
import lombok.Data;

import java.util.List;

@Data
public class ArtistImportResult {
    private List<Artist> artists;
    private int createdCount;
    private int existingCount;
}
