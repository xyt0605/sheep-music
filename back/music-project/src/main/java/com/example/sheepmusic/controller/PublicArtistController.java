package com.example.sheepmusic.controller;

import com.example.sheepmusic.common.Result;
import com.example.sheepmusic.entity.Artist;
import com.example.sheepmusic.service.ArtistService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 公共歌手接口（所有登录用户可访问）
 */
@Api(tags = "歌手浏览")
@RestController
@RequestMapping("/api/artists")
@CrossOrigin
public class PublicArtistController {
    
    @Autowired
    private ArtistService artistService;
    
    /**
     * 获取所有歌手列表（分页）
     * 供普通用户浏览歌手使用
     */
    @ApiOperation("获取歌手列表（分页）")
    @GetMapping
    public Result<Page<Artist>> getArtists(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "songCount") String sortBy,
            @RequestParam(defaultValue = "desc") String order
    ) {
        try {
            Page<Artist> artists;
            Pageable pageable = PageRequest.of(page, size);
            
            // 如果是按歌曲数量排序，使用特殊查询
            if ("songCount".equalsIgnoreCase(sortBy)) {
                artists = artistService.getArtistsBySongCount(pageable, order);
            } else {
                // 其他排序方式使用默认排序
                Sort sort = order.equalsIgnoreCase("asc") 
                        ? Sort.by(sortBy).ascending() 
                        : Sort.by(sortBy).descending();
                pageable = PageRequest.of(page, size, sort);
                artists = artistService.getArtists(pageable);
            }
            
            return Result.success("查询成功", artists);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }
    
    /**
     * 获取所有歌手（不分页）
     * 用于下拉选择等场景
     */
    @ApiOperation("获取所有歌手（不分页）")
    @GetMapping("/all")
    public Result<List<Artist>> getAllArtists() {
        try {
            List<Artist> artists = artistService.getAllArtists();
            return Result.success("查询成功", artists);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }
    
    /**
     * 获取歌手详情
     */
    @ApiOperation("获取歌手详情")
    @GetMapping("/{id}")
    public Result<Artist> getArtist(@PathVariable Long id) {
        try {
            Artist artist = artistService.getArtist(id);
            return Result.success("查询成功", artist);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }
    
    /**
     * 搜索歌手
     */
    @ApiOperation("搜索歌手")
    @GetMapping("/search")
    public Result<List<Artist>> searchArtists(@RequestParam String keyword) {
        try {
            // 这里假设 ArtistService 有搜索方法，如果没有则需要添加
            List<Artist> artists = artistService.getAllArtists()
                .stream()
                .filter(artist -> 
                    artist.getName().contains(keyword) || 
                    (artist.getDescription() != null && artist.getDescription().contains(keyword))
                )
                .collect(Collectors.toList());
            return Result.success("查询成功", artists);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }
}

