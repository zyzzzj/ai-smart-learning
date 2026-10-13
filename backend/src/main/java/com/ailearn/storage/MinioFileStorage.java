package com.ailearn.storage;

import com.ailearn.common.BusinessException;
import com.ailearn.config.StorageProperties;
import io.minio.BucketExistsArgs;
import io.minio.GetObjectArgs;
import io.minio.GetPresignedObjectUrlArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import io.minio.StatObjectArgs;
import io.minio.http.Method;
import lombok.extern.slf4j.Slf4j;

import java.io.InputStream;
import java.util.concurrent.TimeUnit;

/**
 * MinIO 实现:统一管理视频、图片等学习资源,支持预签名直传与临时下载地址。
 */
@Slf4j
public class MinioFileStorage implements FileStorage {

    private final StorageProperties properties;
    private final MinioClient client;

    public MinioFileStorage(StorageProperties properties) {
        this.properties = properties;
        StorageProperties.Minio minio = properties.getMinio();
        this.client = MinioClient.builder()
                .endpoint(minio.getEndpoint())
                .credentials(minio.getAccessKey(), minio.getSecretKey())
                .build();
    }

    @Override
    public String type() {
        return "minio";
    }

    @Override
    public boolean ensureReady() {
        String bucket = properties.getMinio().getBucket();
        try {
            boolean exists = client.bucketExists(BucketExistsArgs.builder().bucket(bucket).build());
            if (!exists) {
                client.makeBucket(MakeBucketArgs.builder().bucket(bucket).build());
                log.info("MinIO 桶已创建:{}", bucket);
            } else {
                log.info("MinIO 桶已存在:{}", bucket);
            }
            return true;
        } catch (Exception ex) {
            log.warn("MinIO 不可用({}):{}", properties.getMinio().getEndpoint(), ex.getMessage());
            return false;
        }
    }

    @Override
    public String upload(String objectKey, InputStream in, long size, String contentType) {
        try {
            client.putObject(PutObjectArgs.builder()
                    .bucket(properties.getMinio().getBucket())
                    .object(objectKey)
                    .stream(in, size, -1)
                    .contentType(contentType == null ? "application/octet-stream" : contentType)
                    .build());
            return presignedDownloadUrl(objectKey, null);
        } catch (Exception ex) {
            throw new BusinessException("MinIO 上传失败:" + ex.getMessage());
        }
    }

    @Override
    public String presignedUploadUrl(String objectKey, Integer expirySeconds) {
        try {
            return client.getPresignedObjectUrl(GetPresignedObjectUrlArgs.builder()
                    .method(Method.PUT)
                    .bucket(properties.getMinio().getBucket())
                    .object(objectKey)
                    .expiry(expiry(expirySeconds), TimeUnit.SECONDS)
                    .build());
        } catch (Exception ex) {
            throw new BusinessException("生成上传地址失败:" + ex.getMessage());
        }
    }

    @Override
    public String presignedDownloadUrl(String objectKey, Integer expirySeconds) {
        try {
            return client.getPresignedObjectUrl(GetPresignedObjectUrlArgs.builder()
                    .method(Method.GET)
                    .bucket(properties.getMinio().getBucket())
                    .object(objectKey)
                    .expiry(expiry(expirySeconds), TimeUnit.SECONDS)
                    .build());
        } catch (Exception ex) {
            throw new BusinessException("生成下载地址失败:" + ex.getMessage());
        }
    }

    @Override
    public InputStream download(String objectKey) {
        try {
            return client.getObject(GetObjectArgs.builder()
                    .bucket(properties.getMinio().getBucket())
                    .object(objectKey)
                    .build());
        } catch (Exception ex) {
            throw new BusinessException("MinIO 读取失败:" + ex.getMessage());
        }
    }

    @Override
    public void delete(String objectKey) {
        try {
            client.removeObject(RemoveObjectArgs.builder()
                    .bucket(properties.getMinio().getBucket())
                    .object(objectKey)
                    .build());
        } catch (Exception ex) {
            log.warn("MinIO 删除失败:{}", ex.getMessage());
        }
    }

    @Override
    public boolean exists(String objectKey) {
        try {
            client.statObject(StatObjectArgs.builder()
                    .bucket(properties.getMinio().getBucket())
                    .object(objectKey)
                    .build());
            return true;
        } catch (Exception ex) {
            return false;
        }
    }

    private int expiry(Integer expirySeconds) {
        int configured = properties.getMinio().getPresignedExpirySeconds();
        return expirySeconds == null || expirySeconds <= 0 ? configured : expirySeconds;
    }
}
