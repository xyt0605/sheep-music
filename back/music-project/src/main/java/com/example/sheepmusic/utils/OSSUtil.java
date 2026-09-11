package com.example.sheepmusic.utils;

import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import com.aliyun.oss.model.ObjectMetadata;
import com.aliyun.oss.model.PutObjectRequest;
import com.example.sheepmusic.service.SystemConfigService;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.UUID;

/**
 * 阿里云OSS工具类
 * 系统设置 v1：配置改为运行时从 SystemConfigService 解析（面板保存即生效），yml 仅作缺省
 */
@Component
public class OSSUtil {

    private final SystemConfigService systemConfigService;

    public OSSUtil(SystemConfigService systemConfigService) {
        this.systemConfigService = systemConfigService;
    }

    /**
     * 把 OSS SDK 异常翻译为可操作的中文提示（含下一步去哪修）
     */
    public static String friendlyOssError(Exception e) {
        String raw = String.valueOf(e.getMessage());
        if (raw.contains("InvalidAccessKeyId")) {
            return "OSS AccessKey 无效（不存在或已被禁用），请到 管理后台 → 系统设置 更新 AccessKeyId";
        }
        if (raw.contains("SignatureDoesNotMatch")) {
            return "OSS AccessKeySecret 不正确，请到 管理后台 → 系统设置 检查";
        }
        if (raw.contains("NoSuchBucket")) {
            return "OSS Bucket 不存在，请到 管理后台 → 系统设置 检查 BucketName";
        }
        if (raw.contains("AccessDenied")) {
            return "OSS 拒绝访问（RAM 子账号权限不足或 Bucket 禁写），请检查该 AccessKey 的权限";
        }
        String lower = raw.toLowerCase();
        if (lower.contains("unknownhost") || lower.contains("connect") || lower.contains("timeout")) {
            return "OSS 连接失败（网络或 Endpoint 不对），请到 管理后台 → 系统设置 检查 Endpoint";
        }
        int cut = Math.min(200, raw.length());
        return "OSS 操作失败：" + raw.substring(0, cut);
    }

    /**
     * 上传文件到OSS
     * @param file 文件
     * @param folder 文件夹路径（如：avatar/）
     * @return 文件访问URL
     */
    public String uploadFile(MultipartFile file, String folder) throws IOException {
        // 1. 获取原始文件名
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null) {
            throw new RuntimeException("文件名不能为空");
        }

        // 2. 获取文件扩展名（无扩展名时置空，避免越界异常）
        int dotIndex = originalFilename.lastIndexOf('.');
        String extension = dotIndex >= 0 ? originalFilename.substring(dotIndex) : "";

        // 3. 生成唯一文件名：folder + UUID + 扩展名
        String fileName = folder + UUID.randomUUID() + extension;

        SystemConfigService.OssProps props = systemConfigService.resolveOss();
        if (props.accessKeyId() == null || props.accessKeySecret() == null) {
            throw new RuntimeException("OSS 尚未配置有效密钥，请到 管理后台 → 系统设置 完成 OSS 配置");
        }

        // 4. 创建OSSClient实例
        OSS ossClient = new OSSClientBuilder().build(props.endpoint(), props.accessKeyId(), props.accessKeySecret());

        InputStream inputStream = null;
        try {
            // 5. 将文件读取为字节数组（解决输入流无法重置的问题）
            byte[] fileBytes = file.getBytes();
            inputStream = new ByteArrayInputStream(fileBytes);

            // 6. 设置对象元数据
            ObjectMetadata metadata = new ObjectMetadata();
            metadata.setContentLength(fileBytes.length);
            metadata.setContentType(file.getContentType());

            // 7. 创建上传请求
            PutObjectRequest putObjectRequest = new PutObjectRequest(props.bucketName(), fileName, inputStream, metadata);

            // 8. 上传文件
            ossClient.putObject(putObjectRequest);

            // 9. 返回文件访问URL
            return props.urlPrefix() + fileName;

        } catch (Exception e) {
            throw new RuntimeException(friendlyOssError(e), e);
        } finally {
            // 10. 关闭输入流
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            // 11. 关闭OSSClient
            if (ossClient != null) {
                ossClient.shutdown();
            }
        }
    }

    /**
     * 删除OSS文件
     * @param fileUrl 文件URL
     */
    public void deleteFile(String fileUrl) {
        if (fileUrl == null || fileUrl.isEmpty()) {
            return;
        }

        SystemConfigService.OssProps props = systemConfigService.resolveOss();

        // 从URL中提取文件路径
        String fileName = fileUrl.replace(props.urlPrefix(), "");

        // 创建OSSClient实例
        OSS ossClient = new OSSClientBuilder().build(props.endpoint(), props.accessKeyId(), props.accessKeySecret());

        try {
            // 删除文件
            ossClient.deleteObject(props.bucketName(), fileName);
        } finally {
            // 关闭OSSClient
            if (ossClient != null) {
                ossClient.shutdown();
            }
        }
    }
}
