package com.example.sheepmusic.service;

import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import com.aliyun.oss.model.BucketInfo;
import com.example.sheepmusic.agent.AiCryptoUtil;
import com.example.sheepmusic.entity.SystemConfig;
import com.example.sheepmusic.repository.SystemConfigRepository;
import com.example.sheepmusic.utils.OSSUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * 系统配置服务（系统设置 v1）：OSS 配置的运行时解析/保存/重置/连接测试
 * DB 行覆盖 application.yml 默认值；密钥类字段加密落库、永不回传明文
 */
@Slf4j
@Service
public class SystemConfigService {

    private static final String K_ENDPOINT = "oss.endpoint";
    private static final String K_AK = "oss.accessKeyId";
    private static final String K_SK = "oss.accessKeySecret";
    private static final String K_BUCKET = "oss.bucketName";
    private static final String K_PREFIX = "oss.urlPrefix";

    private final SystemConfigRepository repo;
    private final AiCryptoUtil crypto;

    @Value("${aliyun.oss.endpoint:oss-cn-hangzhou.aliyuncs.com}")
    private String defEndpoint;
    @Value("${aliyun.oss.bucketName:sheepmusic}")
    private String defBucket;
    @Value("${aliyun.oss.urlPrefix:https://sheepmusic.oss-cn-hangzhou.aliyuncs.com/}")
    private String defUrlPrefix;

    public SystemConfigService(SystemConfigRepository repo, AiCryptoUtil crypto) {
        this.repo = repo;
        this.crypto = crypto;
    }

    /** OSS 运行时配置（DB 行优先，缺省回落 yml） */
    public record OssProps(String endpoint, String accessKeyId, String accessKeySecret,
                           String bucketName, String urlPrefix, boolean fromPanel) {
    }

    /**
     * OSSUtil 每次上传/删除前调用；管理端低频写，直接查表不缓存
     */
    public OssProps resolveOss() {
        Map<String, String> rows = ossRows();
        String ak = decryptQuietly(rows.get(K_AK));
        String sk = decryptQuietly(rows.get(K_SK));
        boolean fromPanel = ak != null && sk != null;
        return new OssProps(
                rows.getOrDefault(K_ENDPOINT, defEndpoint),
                ak,
                sk,
                rows.getOrDefault(K_BUCKET, defBucket),
                rows.getOrDefault(K_PREFIX, defUrlPrefix),
                fromPanel);
    }

    /** 脱敏状态视图（面板状态行） */
    public Map<String, Object> ossStatus() {
        Map<String, String> rows = ossRows();
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("configured", rows.containsKey(K_AK) && rows.containsKey(K_SK));
        out.put("endpoint", rows.getOrDefault(K_ENDPOINT, defEndpoint));
        out.put("bucketName", rows.getOrDefault(K_BUCKET, defBucket));
        out.put("urlPrefix", rows.getOrDefault(K_PREFIX, defUrlPrefix));
        out.put("accessKeyIdMasked", mask(decryptQuietly(rows.get(K_AK))));
        out.put("hasSecret", rows.containsKey(K_SK));
        out.put("defaultEndpoint", defEndpoint);
        out.put("defaultBucketName", defBucket);
        out.put("defaultUrlPrefix", defUrlPrefix);
        return out;
    }

    /**
     * 保存 OSS 配置：Secret 留空且已有值时沿用旧值（仅改其余字段）；首次必填全部
     */
    @Transactional
    public void saveOss(String endpoint, String accessKeyId, String accessKeySecret,
                        String bucketName, String urlPrefix) {
        Map<String, String> rows = ossRows();
        boolean hasAk = accessKeyId != null && !accessKeyId.isBlank();
        boolean hasSk = accessKeySecret != null && !accessKeySecret.isBlank();
        if ((!hasAk && !rows.containsKey(K_AK)) || (!hasSk && !rows.containsKey(K_SK))) {
            throw new IllegalArgumentException("AccessKeyId 和 AccessKeySecret 首次必须都填写");
        }
        upsert(K_AK, hasAk ? crypto.encrypt(accessKeyId.trim()) : rows.get(K_AK));
        upsert(K_SK, hasSk ? crypto.encrypt(accessKeySecret.trim()) : rows.get(K_SK));
        if (endpoint != null && !endpoint.isBlank()) {
            upsert(K_ENDPOINT, endpoint.trim());
        }
        if (bucketName != null && !bucketName.isBlank()) {
            upsert(K_BUCKET, bucketName.trim());
        }
        if (urlPrefix != null && !urlPrefix.isBlank()) {
            upsert(K_PREFIX, urlPrefix.endsWith("/") ? urlPrefix.trim() : urlPrefix.trim() + "/");
        }
        log.info("OSS 配置已通过系统设置面板更新（运行时生效）");
    }

    /** 恢复默认：删除面板行，回落 application.yml */
    @Transactional
    public void resetOss() {
        repo.deleteByConfigKeyStartingWith("oss.");
    }

    /**
     * 连接测试：真实调用 getBucketInfo；可传覆盖值（不落库）
     */
    public Map<String, Object> testOss(Map<String, String> override) {
        Map<String, String> rows = ossRows();
        String ak = pick(override, "accessKeyId", K_AK, rows, true);
        String sk = pick(override, "accessKeySecret", K_SK, rows, true);
        String endpoint = pick(override, "endpoint", K_ENDPOINT, rows, false);
        String bucket = pick(override, "bucketName", K_BUCKET, rows, false);

        Map<String, Object> out = new LinkedHashMap<>();
        if (ak == null || sk == null) {
            out.put("ok", false);
            out.put("message", "请先填写 AccessKeyId 和 AccessKeySecret");
            return out;
        }
        long t0 = System.currentTimeMillis();
        OSS client = new OSSClientBuilder().build(endpoint, ak, sk);
        try {
            BucketInfo info = client.getBucketInfo(bucket);
            out.put("ok", true);
            out.put("message", "连接成功（bucket: " + info.getBucket().getName() + "，" + info.getBucket().getLocation() + "）");
            out.put("latencyMs", System.currentTimeMillis() - t0);
        } catch (Exception e) {
            out.put("ok", false);
            out.put("message", OSSUtil.friendlyOssError(e));
            out.put("latencyMs", System.currentTimeMillis() - t0);
        } finally {
            client.shutdown();
        }
        return out;
    }

    // ===== helpers =====

    private Map<String, String> ossRows() {
        Map<String, String> m = new LinkedHashMap<>();
        repo.findByConfigKeyStartingWith("oss.")
                .forEach(c -> m.put(c.getConfigKey(), c.getConfigValue()));
        return m;
    }

    private void upsert(String key, String value) {
        SystemConfig c = repo.findByConfigKey(key).orElseGet(() -> {
            SystemConfig n = new SystemConfig();
            n.setConfigKey(key);
            return n;
        });
        c.setConfigValue(value);
        c.setUpdateTime(LocalDateTime.now());
        repo.save(c);
    }

    private String decryptQuietly(String enc) {
        if (enc == null || enc.isBlank()) {
            return null;
        }
        try {
            return crypto.decrypt(enc);
        } catch (Exception e) {
            log.error("系统配置解密失败 key 派生材料变更？", e);
            return null;
        }
    }

    private String pick(Map<String, String> override, String name, String key, Map<String, String> rows, boolean encrypted) {
        String v = override == null ? null : override.get(name);
        if (v != null && !v.isBlank()) {
            return v.trim();
        }
        String saved = rows.get(key);
        if (saved == null) {
            return null;
        }
        return encrypted ? decryptQuietly(saved) : saved;
    }

    private String mask(String plain) {
        if (plain == null) {
            return null;
        }
        if (plain.length() <= 8) {
            return "****";
        }
        return plain.substring(0, 4) + "****" + plain.substring(plain.length() - 4);
    }
}
