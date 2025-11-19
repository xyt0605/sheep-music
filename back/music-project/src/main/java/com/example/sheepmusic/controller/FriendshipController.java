package com.example.sheepmusic.controller;

import com.example.sheepmusic.common.Result;
import com.example.sheepmusic.dto.FriendRequest;
import com.example.sheepmusic.entity.Friendship;
import com.example.sheepmusic.entity.User;
import com.example.sheepmusic.service.FriendshipService;
import com.example.sheepmusic.utils.JwtUtil;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import java.util.List;
import java.util.Map;

/**
 * 好友Controller
 */
@Api(tags = "好友管理")
@RestController
@RequestMapping("/friend")
@CrossOrigin
public class FriendshipController {
    
    @Autowired
    private FriendshipService friendshipService;
    
    @Autowired
    private JwtUtil jwtUtil;
    
    /**
     * 发送好友请求
     */
    @ApiOperation("发送好友请求")
    @PostMapping("/request")
    public Result<Friendship> sendFriendRequest(@Valid @RequestBody FriendRequest request,
                                                HttpServletRequest httpRequest) {
        try {
            Long userId = jwtUtil.getUserIdFromRequest(httpRequest);
            
            Friendship friendship = friendshipService.sendFriendRequest(
                userId,
                request.getFriendId(),
                request.getRemark()
            );
            
            return Result.success("好友请求已发送", friendship);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }
    
    /**
     * 接受好友请求
     */
    @ApiOperation("接受好友请求")
    @PostMapping("/accept/{friendshipId}")
    public Result<Void> acceptFriendRequest(@PathVariable Long friendshipId,
                                            HttpServletRequest request) {
        try {
            Long userId = jwtUtil.getUserIdFromRequest(request);
            friendshipService.acceptFriendRequest(friendshipId, userId);
            
            Result<Void> result = Result.success();
            result.setMessage("已接受好友请求");
            return result;
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }
    
    /**
     * 拒绝好友请求
     */
    @ApiOperation("拒绝好友请求")
    @PostMapping("/reject/{friendshipId}")
    public Result<Void> rejectFriendRequest(@PathVariable Long friendshipId,
                                            HttpServletRequest request) {
        try {
            Long userId = jwtUtil.getUserIdFromRequest(request);
            friendshipService.rejectFriendRequest(friendshipId, userId);
            
            Result<Void> result = Result.success();
            result.setMessage("已拒绝好友请求");
            return result;
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }
    
    /**
     * 删除好友
     */
    @ApiOperation("删除好友")
    @DeleteMapping("/{friendshipId}")
    public Result<Void> deleteFriend(@PathVariable Long friendshipId,
                                     HttpServletRequest request) {
        try {
            Long userId = jwtUtil.getUserIdFromRequest(request);
            friendshipService.deleteFriend(friendshipId, userId);
            
            Result<Void> result = Result.success();
            result.setMessage("已删除好友");
            return result;
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }
    
    /**
     * 获取好友列表
     */
    @ApiOperation("获取好友列表")
    @GetMapping("/list")
    public Result<List<Friendship>> getFriendList(HttpServletRequest request) {
        try {
            Long userId = jwtUtil.getUserIdFromRequest(request);
            List<Friendship> friends = friendshipService.getFriendList(userId);
            return Result.success("查询成功", friends);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }
    
    /**
     * 获取好友请求列表
     */
    @ApiOperation("获取好友请求列表")
    @GetMapping("/requests")
    public Result<List<Friendship>> getFriendRequests(HttpServletRequest request) {
        try {
            Long userId = jwtUtil.getUserIdFromRequest(request);
            List<Friendship> requests = friendshipService.getFriendRequests(userId);
            return Result.success("查询成功", requests);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }
    
    /**
     * 搜索用户
     */
    @ApiOperation("搜索用户")
    @GetMapping("/search")
    public Result<List<User>> searchUsers(@RequestParam String keyword,
                                          HttpServletRequest request) {
        try {
            Long userId = jwtUtil.getUserIdFromRequest(request);
            List<User> users = friendshipService.searchUsers(keyword, userId);
            return Result.success("查询成功", users);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }
    
    /**
     * 设置备注名
     */
    @ApiOperation("设置备注名")
    @PutMapping("/{friendshipId}/remark")
    public Result<Void> setRemark(@PathVariable Long friendshipId,
                                  @RequestBody Map<String, String> body,
                                  HttpServletRequest request) {
        try {
            Long userId = jwtUtil.getUserIdFromRequest(request);
            String remark = body.get("remark");
            friendshipService.setRemark(friendshipId, userId, remark);
            
            Result<Void> result = Result.success();
            result.setMessage("备注名设置成功");
            return result;
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }
    
    /**
     * 检查是否为好友
     */
    @ApiOperation("检查是否为好友")
    @GetMapping("/check/{friendId}")
    public Result<Boolean> areFriends(@PathVariable Long friendId,
                                      HttpServletRequest request) {
        try {
            Long userId = jwtUtil.getUserIdFromRequest(request);
            boolean areFriends = friendshipService.areFriends(userId, friendId);
            return Result.success("查询成功", areFriends);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }
}

