package com.example.sheepmusic.service;

import com.example.sheepmusic.entity.Playlist;
import com.example.sheepmusic.entity.PlaylistShare;
import com.example.sheepmusic.entity.Song;
import com.example.sheepmusic.entity.SongShare;
import com.example.sheepmusic.entity.User;
import com.example.sheepmusic.repository.PlaylistRepository;
import com.example.sheepmusic.repository.PlaylistShareRepository;
import com.example.sheepmusic.repository.SongRepository;
import com.example.sheepmusic.repository.SongShareRepository;
import com.example.sheepmusic.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 分享Service
 */
@Service
public class ShareService {
    
    @Autowired
    private PlaylistShareRepository shareRepository;
    
    @Autowired
    private SongShareRepository songShareRepository;
    
    @Autowired
    private PlaylistRepository playlistRepository;
    
    @Autowired
    private SongRepository songRepository;
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private FriendshipService friendshipService;
    
    /**
     * 分享歌单
     */
    @Transactional
    public PlaylistShare sharePlaylist(Long playlistId, Long userId, String description) {
        Playlist playlist = playlistRepository.findById(playlistId)
            .orElseThrow(() -> new RuntimeException("歌单不存在"));
        
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new RuntimeException("用户不存在"));
        
        // 检查权限（只能分享自己的歌单或公开歌单）
        if (!playlist.getUserId().equals(userId) && !playlist.getIsPublic()) {
            throw new RuntimeException("无权分享该歌单");
        }
        
        PlaylistShare share = new PlaylistShare();
        share.setPlaylistId(playlistId);
        share.setUserId(userId);
        share.setDescription(description);
        share.setViewCount(0);
        share.setCollectCount(0);
        
        // 冗余信息
        share.setPlaylistName(playlist.getName());
        share.setPlaylistCover(playlist.getCover());
        share.setUserName(user.getNickname());
        share.setUserAvatar(user.getAvatar());
        
        return shareRepository.save(share);
    }
    
    /**
     * 获取分享广场（所有公开分享）
     */
    public Page<PlaylistShare> getShareSquare(int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return shareRepository.findAllByOrderByCreateTimeDesc(pageable);
    }
    
    /**
     * 获取热门分享
     */
    public Page<PlaylistShare> getHotShares(int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return shareRepository.findAllByOrderByViewCountDesc(pageable);
    }
    
    /**
     * 获取好友分享
     */
    public List<PlaylistShare> getFriendsShares(Long userId) {
        List<com.example.sheepmusic.entity.Friendship> friends = 
            friendshipService.getFriendList(userId);
        
        List<Long> friendIds = friends.stream()
            .map(f -> f.getFriendId())
            .collect(java.util.stream.Collectors.toList());
        
        if (friendIds.isEmpty()) {
            return new java.util.ArrayList<>();
        }
        
        return shareRepository.findFriendsShares(friendIds);
    }
    
    /**
     * 获取用户的分享
     */
    public List<PlaylistShare> getUserShares(Long userId) {
        return shareRepository.findByUserIdOrderByCreateTimeDesc(userId);
    }
    
    /**
     * 增加浏览次数
     */
    @Transactional
    public void incrementViewCount(Long shareId) {
        PlaylistShare share = shareRepository.findById(shareId)
            .orElseThrow(() -> new RuntimeException("分享不存在"));
        
        share.setViewCount(share.getViewCount() + 1);
        shareRepository.save(share);
    }
    
    /**
     * 收藏分享的歌单
     */
    @Transactional
    public void collectSharedPlaylist(Long shareId, Long userId) {
        PlaylistShare share = shareRepository.findById(shareId)
            .orElseThrow(() -> new RuntimeException("分享不存在"));
        
        // 增加收藏次数
        share.setCollectCount(share.getCollectCount() + 1);
        shareRepository.save(share);
        
        // TODO: 实际收藏歌单逻辑（复制歌单到用户的歌单列表）
    }
    
    /**
     * 删除分享
     */
    @Transactional
    public void deleteShare(Long shareId, Long userId) {
        PlaylistShare share = shareRepository.findById(shareId)
            .orElseThrow(() -> new RuntimeException("分享不存在"));
        
        if (!share.getUserId().equals(userId)) {
            throw new RuntimeException("无权删除该分享");
        }
        
        shareRepository.delete(share);
    }
    
    // ==================== 歌曲分享相关方法 ====================
    
    /**
     * 分享歌曲
     */
    @Transactional
    public SongShare shareSong(Long songId, Long userId, String description) {
        Song song = songRepository.findById(songId)
            .orElseThrow(() -> new RuntimeException("歌曲不存在"));
        
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new RuntimeException("用户不存在"));
        
        SongShare share = new SongShare();
        share.setSongId(songId);
        share.setUserId(userId);
        share.setDescription(description);
        share.setViewCount(0);
        share.setLikeCount(0);
        
        // 冗余信息
        share.setSongName(song.getTitle());
        share.setSongCover(song.getCover());
        
        // 处理歌手信息（多个歌手用 / 分隔）
        String artistName = "未知歌手";
        if (song.getArtists() != null && !song.getArtists().isEmpty()) {
            artistName = song.getArtists().stream()
                .map(artist -> artist.getName())
                .collect(java.util.stream.Collectors.joining(" / "));
        }
        share.setArtistName(artistName);
        
        share.setUserName(user.getNickname());
        share.setUserAvatar(user.getAvatar());
        
        return songShareRepository.save(share);
    }
    
    /**
     * 获取歌曲分享广场
     */
    public Page<SongShare> getSongShareSquare(int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return songShareRepository.findAllByOrderByCreateTimeDesc(pageable);
    }
    
    /**
     * 获取热门歌曲分享
     */
    public Page<SongShare> getHotSongShares(int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return songShareRepository.findAllByOrderByViewCountDesc(pageable);
    }
    
    /**
     * 获取好友的歌曲分享
     */
    public List<SongShare> getFriendsSongShares(Long userId) {
        List<com.example.sheepmusic.entity.Friendship> friends = 
            friendshipService.getFriendList(userId);
        
        List<Long> friendIds = friends.stream()
            .map(f -> f.getFriendId())
            .collect(java.util.stream.Collectors.toList());
        
        if (friendIds.isEmpty()) {
            return new java.util.ArrayList<>();
        }
        
        return songShareRepository.findFriendsShares(friendIds);
    }
    
    /**
     * 获取用户的歌曲分享
     */
    public List<SongShare> getUserSongShares(Long userId) {
        return songShareRepository.findByUserIdOrderByCreateTimeDesc(userId);
    }
    
    /**
     * 增加歌曲分享浏览次数
     */
    @Transactional
    public void incrementSongShareViewCount(Long shareId) {
        SongShare share = songShareRepository.findById(shareId)
            .orElseThrow(() -> new RuntimeException("分享不存在"));
        
        share.setViewCount(share.getViewCount() + 1);
        songShareRepository.save(share);
    }
    
    /**
     * 点赞歌曲分享
     */
    @Transactional
    public void likeSongShare(Long shareId, Long userId) {
        SongShare share = songShareRepository.findById(shareId)
            .orElseThrow(() -> new RuntimeException("分享不存在"));
        
        // 增加点赞次数
        share.setLikeCount(share.getLikeCount() + 1);
        songShareRepository.save(share);
        
        // TODO: 记录用户点赞关系，避免重复点赞
    }
    
    /**
     * 删除歌曲分享
     */
    @Transactional
    public void deleteSongShare(Long shareId, Long userId) {
        SongShare share = songShareRepository.findById(shareId)
            .orElseThrow(() -> new RuntimeException("分享不存在"));
        
        if (!share.getUserId().equals(userId)) {
            throw new RuntimeException("无权删除该分享");
        }
        
        songShareRepository.delete(share);
    }
}

