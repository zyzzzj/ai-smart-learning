package com.ailearn.config;

import com.ailearn.storage.FileStorage;
import com.ailearn.storage.LocalFileStorage;
import com.ailearn.storage.MinioFileStorage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 存储装配:按 <code>storage.type</code> 选择 MinIO 或本地磁盘。
 *
 * <p>关键行为:<b>配置为 minio 但 MinIO 连不上时自动降级为本地磁盘存储</b>,
 * 保证系统在任何环境下都能启动并可用;日志里会明确说明降级原因,
 * 启动 MinIO 后重启即可自动切回对象存储。</p>
 */
@Configuration
@Slf4j
public class StorageConfig {

    @Bean
    public FileStorage fileStorage(StorageProperties properties) {
        if (properties.useMinio()) {
            MinioFileStorage minio = new MinioFileStorage(properties);
            if (minio.ensureReady()) {
                log.info("对象存储实现:minio(桶: {})", properties.getMinio().getBucket());
                return minio;
            }
            LocalFileStorage local = new LocalFileStorage(properties);
            local.ensureReady();
            log.warn("已自动降级为本地磁盘存储({})。启动 MinIO 后重启服务即可切回对象存储:"
                    + "D:\\dp\\tools\\minio\\minio.exe server <数据目录> --console-address \":9001\"",
                    properties.getLocal().getDir());
            return local;
        }
        LocalFileStorage local = new LocalFileStorage(properties);
        local.ensureReady();
        log.info("对象存储实现:local(目录: {})", properties.getLocal().getDir());
        return local;
    }
}
