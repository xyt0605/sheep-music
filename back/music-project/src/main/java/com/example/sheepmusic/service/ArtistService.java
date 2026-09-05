package com.example.sheepmusic.service;

import com.example.sheepmusic.dto.ArtistRequest;
import com.example.sheepmusic.dto.ArtistImportResult;
import com.example.sheepmusic.entity.Artist;
import com.example.sheepmusic.repository.ArtistRepository;
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
 * 歌手服务类
 */
@Service
public class ArtistService {
    
    @Autowired
    private ArtistRepository artistRepository;
    
    /**
     * 创建歌手
     */
    public Artist createArtist(ArtistRequest request) {
        // 检查歌手名是否已存在
        String name = request.getName().trim();
        if (artistRepository.findByNameIgnoreCase(name).isPresent()) {
            throw new RuntimeException("歌手名称已存在");
        }
        
        Artist artist = new Artist();
        BeanUtils.copyProperties(request, artist);
        artist.setName(name);
        return artistRepository.save(artist);
    }
    
    /**
     * 更新歌手
     */
    public Artist updateArtist(Long id, ArtistRequest request) {
        Artist artist = artistRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("歌手不存在"));
        
        // 如果修改了名称，检查新名称是否已存在
        String name = request.getName().trim();
        if (!artist.getName().equalsIgnoreCase(name)) {
            if (artistRepository.findByNameIgnoreCase(name).isPresent()) {
                throw new RuntimeException("歌手名称已存在");
            }
        }
        
        BeanUtils.copyProperties(request, artist, "id", "createTime", "updateTime");
        artist.setName(name);
        return artistRepository.save(artist);
    }
    
    /**
     * 删除歌手
     */
    public void deleteArtist(Long id) {
        if (!artistRepository.existsById(id)) {
            throw new RuntimeException("歌手不存在");
        }
        artistRepository.deleteById(id);
    }
    
    /**
     * 获取歌手详情
     */
    public Artist getArtist(Long id) {
        return artistRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("歌手不存在"));
    }
    
    /**
     * 获取所有歌手（分页）
     */
    public Page<Artist> getArtists(Pageable pageable) {
        return getArtists(null, pageable);
    }

    /**
     * 管理端按名称查询歌手，避免前端拉取全表后再过滤。
     */
    public Page<Artist> getArtists(String keyword, Pageable pageable) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return artistRepository.findAll(pageable);
        }
        return artistRepository.findByNameContainingIgnoreCase(keyword.trim(), pageable);
    }
    
    /**
     * 获取所有歌手（不分页）
     */
    public List<Artist> getAllArtists() {
        return artistRepository.findAll();
    }

    /**
     * 按名称批量导入歌手，重复项直接复用已有记录。
     */
    @Transactional
    public ArtistImportResult importArtists(List<String> names) {
        Map<String, Artist> unique = new LinkedHashMap<>();
        int createdCount = 0;
        int existingCount = 0;

        if (names != null) {
            for (String rawName : names) {
                if (rawName == null || rawName.trim().isEmpty()) {
                    continue;
                }
                String name = rawName.trim();
                String key = name.toLowerCase();
                if (unique.containsKey(key)) {
                    continue;
                }
                Artist artist = artistRepository.findByNameIgnoreCase(name).orElse(null);
                if (artist == null) {
                    artist = new Artist();
                    artist.setName(name);
                    artist = artistRepository.save(artist);
                    createdCount++;
                } else {
                    existingCount++;
                }
                unique.put(key, artist);
            }
        }

        if (unique.isEmpty()) {
            throw new RuntimeException("至少提供一个有效的歌手名称");
        }

        ArtistImportResult result = new ArtistImportResult();
        result.setArtists(new ArrayList<>(unique.values()));
        result.setCreatedCount(createdCount);
        result.setExistingCount(existingCount);
        return result;
    }

    /**
     * 导入歌曲时解析歌手名称，不存在则自动创建。
     */
    @Transactional
    public Artist getOrCreateByName(String rawName) {
        if (rawName == null || rawName.trim().isEmpty()) {
            throw new RuntimeException("歌手名称不能为空");
        }
        String name = rawName.trim();
        return artistRepository.findByNameIgnoreCase(name).orElseGet(() -> {
            Artist artist = new Artist();
            artist.setName(name);
            return artistRepository.save(artist);
        });
    }
    
    /**
     * 按歌曲数量排序获取歌手列表
     * @param pageable 分页参数
     * @param order 排序方式：asc-升序，desc-降序
     */
    public Page<Artist> getArtistsBySongCount(Pageable pageable, String order) {
        if ("asc".equalsIgnoreCase(order)) {
            return artistRepository.findAllOrderBySongCountAsc(pageable);
        } else {
            return artistRepository.findAllOrderBySongCountDesc(pageable);
        }
    }
}

