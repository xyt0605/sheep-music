package com.example.sheepmusic.controller;

import com.example.sheepmusic.common.Result;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.web.bind.annotation.*;

/**
 * 管理员控制器
 * 只有管理员可以访问这些接口
 */
@Tag(name = "管理员功能")
@RestController
@RequestMapping("/admin")
@CrossOrigin
public class AdminController {
    
    /**
     * 测试管理员权限
     */
    @Operation(summary = "测试管理员权限")
    @GetMapping("/test")
    public Result<String> testAdmin() {
        return Result.success("恭喜！你有管理员权限", "admin access granted");
    }
    
    /**
     * 获取所有用户列表（示例）
     */
    @Operation(summary = "获取所有用户列表")
    @GetMapping("/users")
    public Result<String> getUserList() {
        return Result.success("用户列表", "user list data");
    }
}
