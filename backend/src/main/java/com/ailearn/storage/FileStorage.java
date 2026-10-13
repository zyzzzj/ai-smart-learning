package com.ailearn.storage;

import java.io.InputStream;

/**
 * 对象存储抽象层:MinIO 与本地磁盘两种实现,通过 <code>storage.type</code> 切换。
 *
 * <p>业务只依赖本接口,因此从本地存储切换到 MinIO(或以后换成 OSS)不需要改业务代码。</p>
 */
public interface FileStorage {

    /** 实现类型:minio / local */
    String type();

    /**
     * 初始化(创建桶 / 目录)。
     *
     * @return true 表示可用;MinIO 连不上时返回 false,由装配层决定是否降级
     */
    boolean ensureReady();

    /** 上传对象,返回可访问地址(MinIO 返回预签名 URL,本地返回直链) */
    String upload(String objectKey, InputStream in, long size, String contentType);

    /** 生成上传用临时地址(前端直传,不经过后端) */
    String presignedUploadUrl(String objectKey, Integer expirySeconds);

    /** 生成下载用临时地址 */
    String presignedDownloadUrl(String objectKey, Integer expirySeconds);

    /** 读取对象(本地存储的下载接口使用) */
    InputStream download(String objectKey);

    void delete(String objectKey);

    boolean exists(String objectKey);
}
