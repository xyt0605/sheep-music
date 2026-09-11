package com.example.sheepmusic.controller;

import com.example.sheepmusic.common.Result;
import com.example.sheepmusic.utils.JwtUtil;
import com.example.sheepmusic.utils.OSSUtil;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import jakarta.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.Map;

/**
 * 文件上传控制器（使用阿里云OSS）
 */
@Tag(name = "文件上传")
@RestController
@RequestMapping("/upload")
@CrossOrigin
public class FileUploadController {

    private static final String IMAGE_EXTENSION_PATTERN = "\\.(jpg|jpeg|png|gif|bmp|webp)$";

    @Autowired
    private OSSUtil ossUtil;

    @Autowired
    private JwtUtil jwtUtil;

    /**
     * 仅管理员可调用（用于封面/音频等管理端上传接口）
     */
    private boolean isAdmin(HttpServletRequest request) {
        String role = jwtUtil.getRoleFromRequest(request);
        return "admin".equalsIgnoreCase(role);
    }

    /**
     * 校验并返回图片扩展名（含点号，全小写），不合法返回 null
     */
    private String getImageExtension(MultipartFile file) {
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null) {
            return null;
        }
        int dotIndex = originalFilename.lastIndexOf('.');
        // 无扩展名的文件直接拒绝，避免 StringIndexOutOfBoundsException
        if (dotIndex < 0) {
            return null;
        }
        String extension = originalFilename.substring(dotIndex).toLowerCase();
        return extension.matches(IMAGE_EXTENSION_PATTERN) ? extension : null;
    }

    /**
     * 上传头像到OSS
     */
    @Operation(summary = "上传头像")
    @PostMapping("/avatar")
    public Result<Map<String, String>> uploadAvatar(@RequestParam("file") MultipartFile file) {
        try {
            // 1. 验证文件
            if (file.isEmpty()) {
                return Result.error("文件不能为空");
            }

            // 2. 验证文件类型（只允许图片）
            String contentType = file.getContentType();
            if (contentType == null || !contentType.startsWith("image/")) {
                return Result.error("只能上传图片文件");
            }

            // 3. 验证扩展名（无扩展名或非法扩展名均拒绝）
            String extension = getImageExtension(file);
            if (extension == null) {
                return Result.error("不支持的图片格式");
            }

            // 4. 验证文件大小（2MB）
            long maxSize = 2 * 1024 * 1024; // 2MB
            if (file.getSize() > maxSize) {
                return Result.error("图片大小不能超过2MB");
            }

            // 5. 上传到OSS（avatar/ 文件夹）
            String url = ossUtil.uploadFile(file, "avatar/");

            // 6. 返回结果
            Map<String, String> result = new HashMap<>();
            result.put("url", url);
            result.put("filename", url.substring(url.lastIndexOf("/") + 1));

            return Result.success("上传成功", result);

        } catch (Exception e) {
            e.printStackTrace();
            return Result.error("文件上传失败：" + e.getMessage());
        }
    }

    /**
     * 上传音乐封面到OSS（仅管理员）
     */
    @Operation(summary = "上传音乐封面")
    @PostMapping("/cover")
    public Result<Map<String, String>> uploadCover(@RequestParam("file") MultipartFile file,
                                                   HttpServletRequest request) {
        try {
            if (!isAdmin(request)) {
                return Result.error("无权限上传封面");
            }

            if (file.isEmpty()) {
                return Result.error("文件不能为空");
            }

            String contentType = file.getContentType();
            if (contentType == null || !contentType.startsWith("image/")) {
                return Result.error("只能上传图片文件");
            }

            // 封面此前未做扩展名与大小校验，这里与头像保持同等强度的校验（放宽到5MB）
            String extension = getImageExtension(file);
            if (extension == null) {
                return Result.error("不支持的图片格式");
            }
            long maxSize = 5 * 1024 * 1024; // 5MB
            if (file.getSize() > maxSize) {
                return Result.error("图片大小不能超过5MB");
            }

            // 上传到OSS（cover/ 文件夹）
            String url = ossUtil.uploadFile(file, "cover/");

            Map<String, String> result = new HashMap<>();
            result.put("url", url);
            result.put("filename", url.substring(url.lastIndexOf("/") + 1));

            return Result.success("上传成功", result);

        } catch (Exception e) {
            e.printStackTrace();
            return Result.error("文件上传失败：" + e.getMessage());
        }
    }

    /**
     * 上传音乐文件到OSS（仅管理员）
     */
    @Operation(summary = "上传音乐文件")
    @PostMapping("/music")
    public Result<Map<String, String>> uploadMusic(@RequestParam("file") MultipartFile file,
                                                   HttpServletRequest request) {
        try {
            if (!isAdmin(request)) {
                return Result.error("无权限上传音频");
            }

            if (file.isEmpty()) {
                return Result.error("文件不能为空");
            }

            String contentType = file.getContentType();
            if (contentType == null || !contentType.startsWith("audio/")) {
                return Result.error("只能上传音频文件");
            }

            // 验证文件大小（30MB）
            long maxSize = 30 * 1024 * 1024; // 30MB
            if (file.getSize() > maxSize) {
                return Result.error("音频文件大小不能超过30MB");
            }

            // 上传到OSS（music/ 文件夹）
            String url = ossUtil.uploadFile(file, "music/");

            Map<String, String> result = new HashMap<>();
            result.put("url", url);
            result.put("filename", url.substring(url.lastIndexOf("/") + 1));

            return Result.success("上传成功", result);

        } catch (Exception e) {
            e.printStackTrace();
            return Result.error("文件上传失败：" + e.getMessage());
        }
    }

    /**
     * 上传婉婉小屋素材到OSS（仅管理员）：图片 ≤10MB，视频（mp4/mov/webm）≤200MB
     */
    @Operation(summary = "上传婉婉小屋素材")
    @PostMapping("/memory")
    public Result<Map<String, String>> uploadMemory(@RequestParam("file") MultipartFile file,
                                                    HttpServletRequest request) {
        try {
            if (!isAdmin(request)) {
                return Result.error("无权限上传小屋素材");
            }
            if (file.isEmpty()) {
                return Result.error("文件不能为空");
            }

            String contentType = file.getContentType();
            String extension = getImageExtension(file);
            boolean isImage = contentType != null && contentType.startsWith("image/") && extension != null;
            boolean isVideo = contentType != null && contentType.matches("video/mp4|video/quicktime|video/webm")
                    && file.getOriginalFilename() != null
                    && file.getOriginalFilename().toLowerCase().matches(".*\\.(mp4|mov|webm)$");

            if (isImage) {
                long maxSize = 10 * 1024 * 1024; // 10MB
                if (file.getSize() > maxSize) {
                    return Result.error("图片大小不能超过10MB");
                }
            } else if (isVideo) {
                long maxSize = 200 * 1024 * 1024; // 200MB
                if (file.getSize() > maxSize) {
                    return Result.error("视频大小不能超过200MB（建议 1 分钟内的 1080p 素材）");
                }
            } else {
                return Result.error("仅支持图片（jpg/png/gif/webp 等）或视频（mp4/mov/webm）");
            }

            // 上传到OSS（memory/ 文件夹）
            String url = ossUtil.uploadFile(file, "memory/");

            Map<String, String> result = new HashMap<>();
            result.put("url", url);
            result.put("filename", url.substring(url.lastIndexOf("/") + 1));
            result.put("kind", isImage ? "photo" : "video");

            return Result.success("上传成功", result);

        } catch (Exception e) {
            e.printStackTrace();
            return Result.error("文件上传失败：" + e.getMessage());
        }
    }
}
