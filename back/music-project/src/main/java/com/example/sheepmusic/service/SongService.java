package com.example.sheepmusic.service;

import com.example.sheepmusic.controller.SongController;
import com.example.sheepmusic.dto.SongImportRequest;
import com.example.sheepmusic.dto.SongRequest;
import com.example.sheepmusic.entity.Artist;
import com.example.sheepmusic.entity.Song;
import com.example.sheepmusic.repository.ArtistRepository;
import com.example.sheepmusic.repository.SongRepository;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 歌曲服务类
 */
@Service
public class SongService {
    
    @Autowired
    private SongRepository songRepository;
    
    @Autowired
    private ArtistRepository artistRepository;

    @Autowired
    private ArtistService artistService;
    
    /**
     * 创建歌曲（支持多歌手）
     */
    @Transactional
    public Song createSong(SongRequest request) {
        Song song = new Song();
        BeanUtils.copyProperties(request, song, "artistIds");
        
        // 设置默认状态
        if (song.getStatus() == null) {
            song.setStatus(1);
        }
        
        // 关联歌手
        if (request.getArtistIds() != null && !request.getArtistIds().isEmpty()) {
            List<Artist> artists = new ArrayList<>();
            for (Long artistId : request.getArtistIds()) {
                Artist artist = artistRepository.findById(artistId)
                        .orElseThrow(() -> new RuntimeException("歌手ID " + artistId + " 不存在"));
                artists.add(artist);
            }
            song.setArtists(artists);
        }
        
        return songRepository.save(song);
    }

    /**
     * 管理端一站式导入歌曲：已有歌手按 ID 关联，不存在的歌手按名称自动创建。
     */
    @Transactional
    public Song importSong(SongImportRequest request) {
        Song song = new Song();
        BeanUtils.copyProperties(request, song, "artistIds", "artistNames");
        if (song.getStatus() == null) {
            song.setStatus(1);
        }

        Map<Long, Artist> resolvedArtists = new LinkedHashMap<>();
        if (request.getArtistIds() != null) {
            for (Long artistId : request.getArtistIds()) {
                if (artistId == null) {
                    continue;
                }
                Artist artist = artistRepository.findById(artistId)
                        .orElseThrow(() -> new RuntimeException("歌手ID " + artistId + " 不存在"));
                resolvedArtists.put(artist.getId(), artist);
            }
        }
        if (request.getArtistNames() != null) {
            for (String name : request.getArtistNames()) {
                Artist artist = artistService.getOrCreateByName(name);
                resolvedArtists.put(artist.getId(), artist);
            }
        }
        if (resolvedArtists.isEmpty()) {
            throw new RuntimeException("至少需要选择或填写一位歌手");
        }
        song.setArtists(new ArrayList<>(resolvedArtists.values()));
        return songRepository.save(song);
    }
    
    /**
     * 更新歌曲（支持多歌手）
     */
    @Transactional
    public Song updateSong(Long id, SongRequest request) {
        Song song = songRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("歌曲不存在"));

        BeanUtils.copyProperties(request, song, "id", "playCount", "createTime", "updateTime", "artistIds", "artists");

        // status 为可选字段，请求未携带时保留原值，避免违反非空约束
        if (song.getStatus() == null) {
            song.setStatus(1);
        }

        // 更新歌手关联
        if (request.getArtistIds() != null && !request.getArtistIds().isEmpty()) {
            List<Artist> artists = new ArrayList<>();
            for (Long artistId : request.getArtistIds()) {
                Artist artist = artistRepository.findById(artistId)
                        .orElseThrow(() -> new RuntimeException("歌手ID " + artistId + " 不存在"));
                artists.add(artist);
            }
            song.setArtists(artists);
        }
        
        return songRepository.save(song);
    }
    
    /**
     * 删除歌曲
     */
    public void deleteSong(Long id) {
        if (!songRepository.existsById(id)) {
            throw new RuntimeException("歌曲不存在");
        }
        songRepository.deleteById(id);
    }
    
    /**
     * 获取歌曲详情
     */
    public Song getSong(Long id) {
        return songRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("歌曲不存在"));
    }
    
    /**
     * 获取所有歌曲（分页）
     */
    public Page<Song> getSongs(Pageable pageable) {
        return songRepository.findAll(pageable);
    }

    /**
     * 管理端歌曲查询，支持关键词和状态组合筛选。
     */
    public Page<Song> getSongs(String keyword, Integer status, Pageable pageable) {
        boolean hasKeyword = keyword != null && !keyword.trim().isEmpty();
        if (hasKeyword && status != null) {
            return songRepository.searchByKeywordAndStatus(keyword.trim(), status, pageable);
        }
        if (hasKeyword) {
            return songRepository.searchByKeyword(keyword.trim(), pageable);
        }
        if (status != null) {
            return songRepository.findByStatus(status, pageable);
        }
        return songRepository.findAll(pageable);
    }
    
    /**
     * 根据状态获取歌曲（分页）
     */
    public Page<Song> getSongsByStatus(Integer status, Pageable pageable) {
        return songRepository.findByStatus(status, pageable);
    }
    
    /**
     * 搜索歌曲
     */
    public Page<Song> searchSongs(String keyword, Pageable pageable) {
        return songRepository.searchByKeyword(keyword, pageable);
    }
    
    /**
     * 获取热门歌曲
     */
    public Page<Song> getHotSongs(Pageable pageable) {
        return songRepository.findByStatusOrderByPlayCountDesc(1, pageable);
    }
    
    /**
     * 获取最新歌曲
     */
    public Page<Song> getNewSongs(Pageable pageable) {
        return songRepository.findByStatusOrderByCreateTimeDesc(1, pageable);
    }
    
    /**
     * 根据歌手获取歌曲
     */
    public List<Song> getSongsByArtist(Long artistId) {
        return songRepository.findByArtistId(artistId);
    }
    
    /**
     * 增加播放次数（原子更新，避免并发丢失）
     */
    @Transactional
    public void incrementPlayCount(Long id) {
        if (!songRepository.existsById(id)) {
            throw new RuntimeException("歌曲不存在");
        }
        songRepository.incrementPlayCount(id);
    }
    
    /**
     * 批量更新歌曲类型和语言
     */
    @Transactional
    public void batchUpdateGenreAndLanguage(List<SongController.SongGenreLanguageUpdate> updates) {
        for (SongController.SongGenreLanguageUpdate update : updates) {
            Song song = songRepository.findById(update.getId())
                    .orElseThrow(() -> new RuntimeException("歌曲ID " + update.getId() + " 不存在"));
            
            if (update.getGenre() != null && !update.getGenre().isEmpty()) {
                song.setGenre(update.getGenre());
            }
            
            if (update.getLanguage() != null && !update.getLanguage().isEmpty()) {
                song.setLanguage(update.getLanguage());
            }
            
            songRepository.save(song);
        }
    }
}

