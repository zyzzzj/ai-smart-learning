package com.ailearn.storage;

import com.ailearn.common.BusinessException;
import com.ailearn.common.ForbiddenException;
import com.ailearn.common.NotFoundException;
import com.ailearn.config.StorageProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Base64;
import java.util.Comparator;
import java.util.stream.Stream;

/**
 * 本地磁盘实现(MinIO 不可用时的降级方案,便于离线开发)。
 *
 * <p>没有 MinIO 的预签名能力,这里用 HMAC 签名 + 过期时间模拟临时地址,
 * 由 {@code /api/files/local/**} 接口校验签名后读写文件。</p>
 */
@Slf4j
public class LocalFileStorage implements FileStorage {

    private final StorageProperties properties;
    private final Path root;

    public LocalFileStorage(StorageProperties properties) {
        this.properties = properties;
        this.root = Paths.get(properties.getLocal().getDir()).toAbsolutePath().normalize();
    }

    @Override
    public String type() {
        return "local";
    }

    @Override
    public boolean ensureReady() {
        try {
            Files.createDirectories(root);
            log.info("本地存储目录就绪:{}", root);
            return true;
        } catch (IOException ex) {
            throw new BusinessException("本地存储目录创建失败:" + ex.getMessage());
        }
    }

    @Override
    public String upload(String objectKey, InputStream in, long size, String contentType) {
        try {
            Path target = resolve(objectKey);
            Files.createDirectories(target.getParent());
            Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
            return presignedDownloadUrl(objectKey, null);
        } catch (IOException ex) {
            throw new BusinessException("本地存储写入失败:" + ex.getMessage());
        }
    }

    @Override
    public String presignedUploadUrl(String objectKey, Integer expirySeconds) {
        return sign("upload", objectKey, expirySeconds);
    }

    @Override
    public String presignedDownloadUrl(String objectKey, Integer expirySeconds) {
        return sign("download", objectKey, expirySeconds);
    }

    @Override
    public InputStream download(String objectKey) {
        Path target = resolve(objectKey);
        if (!Files.exists(target)) {
            throw new NotFoundException("文件不存在:" + objectKey);
        }
        try {
            return Files.newInputStream(target);
        } catch (IOException ex) {
            throw new NotFoundException("文件读取失败:" + objectKey);
        }
    }

    @Override
    public void delete(String objectKey) {
        try {
            Files.deleteIfExists(resolve(objectKey));
        } catch (IOException ex) {
            log.warn("本地文件删除失败:{}", ex.getMessage());
        }
    }

    @Override
    public boolean exists(String objectKey) {
        return Files.exists(resolve(objectKey));
    }

    /**
     * 校验签名并返回对象键;过期或签名错误抛 {@link ForbiddenException}(HTTP 403)。
     */
    public String verify(String action, String objectKey, long expireAt, String signature) {
        if (expireAt < System.currentTimeMillis() / 1000) {
            throw new ForbiddenException("链接已过期");
        }
        String expected = hmac(action + ":" + objectKey + ":" + expireAt);
        if (!expected.equals(signature)) {
            throw new ForbiddenException("链接签名无效");
        }
        return objectKey;
    }

    private String sign(String action, String objectKey, Integer expirySeconds) {
        long expireAt = System.currentTimeMillis() / 1000
                + (expirySeconds == null || expirySeconds <= 0
                ? properties.getMinio().getPresignedExpirySeconds() : expirySeconds);
        String signature = hmac(action + ":" + objectKey + ":" + expireAt);
        String prefix = properties.getLocal().getUrlPrefix();
        if (prefix == null || prefix.isBlank()) {
            prefix = "/api/files/local";
        }
        prefix = prefix.replaceAll("/+$", "");
        return prefix + "/" + action + "?key=" + urlEncode(objectKey)
                + "&exp=" + expireAt + "&sig=" + signature;
    }

    private String hmac(String data) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(properties.getMinio().getSecretKey().getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return Base64.getUrlEncoder().withoutPadding()
                    .encodeToString(mac.doFinal(data.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception ex) {
            throw new BusinessException("签名失败:" + ex.getMessage());
        }
    }

    private String urlEncode(String value) {
        return java.net.URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    /**
     * 解析对象键为磁盘路径,并防止路径穿越。
     */
    private Path resolve(String objectKey) {
        Path target = root.resolve(objectKey).normalize();
        if (!target.startsWith(root)) {
            throw new BusinessException("非法的对象键");
        }
        return target;
    }

    /** 供调试:列出本地已存文件 */
    public Stream<Path> listAll() throws IOException {
        if (!Files.exists(root)) {
            return Stream.empty();
        }
        return Files.walk(root).filter(Files::isRegularFile).sorted(Comparator.naturalOrder());
    }

    public String getRoot() {
        return StringUtils.cleanPath(root.toString());
    }
}
