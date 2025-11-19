package com.example.sheepmusic.controller;

import com.example.sheepmusic.common.Result;
import com.example.sheepmusic.dto.ShareRequest;
import com.example.sheepmusic.entity.PlaylistShare;
import com.example.sheepmusic.entity.SongShare;
import com.example.sheepmusic.service.ShareService;
import com.example.sheepmusic.utils.JwtUtil;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import java.util.List;

/**
 * 分享Controller
 */
@Api(tags = "分享管理")
@RestController
@RequestMapping("/share")
@CrossOrigin
public class ShareController {
    
    @Autowired
    private ShareService shareService;
    
    @Autowired
    private JwtUtil jwtUtil;
    
    /**
     * 分享歌单
     */
    @ApiOperation("分享歌单")
    @PostMapping("/playlist")
    public Result<PlaylistShare> sharePlaylist(@Valid @RequestBody ShareRequest request,
                                               HttpServletRequest httpRequest) {
        try {
            Long userId = jwtUtil.getUserIdFromRequest(httpRequest);
            
            PlaylistShare share = shareService.sharePlaylist(
                request.getPlaylistId(),
                userId,
                request.getDescription()
            );
            
            return Result.success("分享成功", share);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }
    
    /**
     * 获取分享广场
     */
    @ApiOperation("获取分享广场")
    @GetMapping("/square")
    public Result<Page<PlaylistShare>> getShareSquare(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        try {
            Page<PlaylistShare> shares = shareService.getShareSquare(page, size);
            return Result.success("查询成功", shares);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }
    
    /**
     * 获取热门分享
     */
    @ApiOperation("获取热门分享")
    @GetMapping("/hot")
    public Result<Page<PlaylistShare>> getHotShares(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        try {
            Page<PlaylistShare> shares = shareService.getHotShares(page, size);
            return Result.success("查询成功", shares);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }
    
    /**
     * 获取好友分享
     */
    @ApiOperation("获取好友分享")
    @GetMapping("/friends")
    public Result<List<PlaylistShare>> getFriendsShares(HttpServletRequest request) {
        try {
            Long userId = jwtUtil.getUserIdFromRequest(request);
            List<PlaylistShare> shares = shareService.getFriendsShares(userId);
            return Result.success("查询成功", shares);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }
    
    /**
     * 获取用户的分享
     */
    @ApiOperation("获取用户的分享")
    @GetMapping("/user/{userId}")
    public Result<List<PlaylistShare>> getUserShares(@PathVariable Long userId) {
        try {
            List<PlaylistShare> shares = shareService.getUserShares(userId);
            return Result.success("查询成功", shares);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }
    
    /**
     * 增加浏览次数
     */
    @ApiOperation("增加浏览次数")
    @PostMapping("/{shareId}/view")
    public Result<Void> incrementViewCount(@PathVariable Long shareId) {
        try {
            shareService.incrementViewCount(shareId);
            
            Result<Void> result = Result.success();
            result.setMessage("操作成功");
            return result;
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }
    
    /**
     * 收藏分享的歌单
     */
    @ApiOperation("收藏分享的歌单")
    @PostMapping("/{shareId}/collect")
    public Result<Void> collectSharedPlaylist(@PathVariable Long shareId,
                                              HttpServletRequest request) {
        try {
            Long userId = jwtUtil.getUserIdFromRequest(request);
            shareService.collectSharedPlaylist(shareId, userId);
            
            Result<Void> result = Result.success();
            result.setMessage("收藏成功");
            return result;
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }
    
    /**
     * 删除分享
     */
    @ApiOperation("删除分享")
    @DeleteMapping("/{shareId}")
    public Result<Void> deleteShare(@PathVariable Long shareId,
                                    HttpServletRequest request) {
        try {
            Long userId = jwtUtil.getUserIdFromRequest(request);
            shareService.deleteShare(shareId, userId);
            
            Result<Void> result = Result.success();
            result.setMessage("删除成功");
            return result;
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }
    
    // ==================== 歌曲分享相关接口 ====================
    
    /**
     * 分享歌曲
     */
    @ApiOperation("分享歌曲")
    @PostMapping("/song")
    public Result<SongShare> shareSong(@Valid @RequestBody ShareRequest request,
                                       HttpServletRequest httpRequest) {
        try {
            Long userId = jwtUtil.getUserIdFromRequest(httpRequest);
            
            SongShare share = shareService.shareSong(
                request.getSongId(),
                userId,
                request.getDescription()
            );
            
            return Result.success("分享成功", share);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }
    
    /**
     * 获取歌曲分享广场
     */
    @ApiOperation("获取歌曲分享广场")
    @GetMapping("/songs")
    public Result<Page<SongShare>> getSongShareSquare(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        try {
            Page<SongShare> shares = shareService.getSongShareSquare(page, size);
            return Result.success("查询成功", shares);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }
    
    /**
     * 获取热门歌曲分享
     */
    @ApiOperation("获取热门歌曲分享")
    @GetMapping("/songs/hot")
    public Result<Page<SongShare>> getHotSongShares(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        try {
            Page<SongShare> shares = shareService.getHotSongShares(page, size);
            return Result.success("查询成功", shares);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }
    
    /**
     * 获取好友的歌曲分享
     */
    @ApiOperation("获取好友的歌曲分享")
    @GetMapping("/songs/friends")
    public Result<List<SongShare>> getFriendsSongShares(HttpServletRequest request) {
        try {
            Long userId = jwtUtil.getUserIdFromRequest(request);
            List<SongShare> shares = shareService.getFriendsSongShares(userId);
            return Result.success("查询成功", shares);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }
    
    /**
     * 获取用户的歌曲分享
     */
    @ApiOperation("获取用户的歌曲分享")
    @GetMapping("/songs/user/{userId}")
    public Result<List<SongShare>> getUserSongShares(@PathVariable Long userId) {
        try {
            List<SongShare> shares = shareService.getUserSongShares(userId);
            return Result.success("查询成功", shares);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }
    
    /**
     * 增加歌曲分享浏览次数
     */
    @ApiOperation("增加歌曲分享浏览次数")
    @PostMapping("/song/{shareId}/view")
    public Result<Void> incrementSongShareViewCount(@PathVariable Long shareId) {
        try {
            shareService.incrementSongShareViewCount(shareId);
            
            Result<Void> result = Result.success();
            result.setMessage("操作成功");
            return result;
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }
    
    /**
     * 点赞歌曲分享
     */
    @ApiOperation("点赞歌曲分享")
    @PostMapping("/song/{shareId}/like")
    public Result<Void> likeSongShare(@PathVariable Long shareId,
                                      HttpServletRequest request) {
        try {
            Long userId = jwtUtil.getUserIdFromRequest(request);
            shareService.likeSongShare(shareId, userId);
            
            Result<Void> result = Result.success();
            result.setMessage("点赞成功");
            return result;
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }
    
    /**
     * 删除歌曲分享
     */
    @ApiOperation("删除歌曲分享")
    @DeleteMapping("/song/{shareId}")
    public Result<Void> deleteSongShare(@PathVariable Long shareId,
                                        HttpServletRequest request) {
        try {
            Long userId = jwtUtil.getUserIdFromRequest(request);
            shareService.deleteSongShare(shareId, userId);
            
            Result<Void> result = Result.success();
            result.setMessage("删除成功");
            return result;
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }
}

