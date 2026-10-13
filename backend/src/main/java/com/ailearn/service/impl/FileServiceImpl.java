package com.ailearn.service.impl;

import com.ailearn.common.BusinessException;
import com.ailearn.config.StorageProperties;
import com.ailearn.entity.SysFile;
import com.ailearn.mapper.SysFileMapper;
import com.ailearn.service.FileService;
import com.ailearn.storage.FileStorage;
import com.ailearn.vo.FileVO;
import com.ailearn.vo.UploadInfoVO;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * 资源管理实现:统一走 {@link FileStorage} 抽象,默认 MinIO,可切本地磁盘。
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class FileServiceImpl implements FileService {

    private static final DateTimeFormatter MONTH_FORMAT = DateTimeFormatter.ofPattern("yyyyMM");

    private final FileStorage fileStorage;
    private final SysFileMapper sysFileMapper;
    private final StorageProperties storageProperties;

    @Override
    public FileVO upload(MultipartFile file, String bizType, Long bizId, Long userId) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException("上传文件不能为空");
        }
        String originalName = file.getOriginalFilename();
        String objectKey = buildObjectKey(bizType, originalName);
        String contentType = file.getContentType();
        try (InputStream in = file.getInputStream()) {
            String url = fileStorage.upload(objectKey, in, file.getSize(), contentType);
            SysFile entity = saveRecord(objectKey, originalName, contentType, file.getSize(), bizType, bizId, userId, url);
            log.info("资源上传成功:type={}, key={}, size={}B", fileStorage.type(), objectKey, file.getSize());
            return toVO(entity);
        } catch (IOException ex) {
            throw new BusinessException("读取上传文件失败:" + ex.getMessage());
        }
    }

    @Override
    public UploadInfoVO presign(String fileName, String contentType, String bizType, Long bizId, Long userId) {
        if (!StringUtils.hasText(fileName)) {
            throw new BusinessException("文件名不能为空");
        }
        String objectKey = buildObjectKey(bizType, fileName);
        int expiry = storageProperties.getMinio().getPresignedExpirySeconds();
        String uploadUrl = fileStorage.presignedUploadUrl(objectKey, expiry);
        String downloadUrl = fileStorage.presignedDownloadUrl(objectKey, expiry);

        SysFile entity = saveRecord(objectKey, fileName, contentType, 0L, bizType, bizId, userId, downloadUrl);

        UploadInfoVO vo = new UploadInfoVO();
        vo.setFileId(entity.getId());
        vo.setStorageType(fileStorage.type());
        vo.setBucket(entity.getBucket());
        vo.setObjectKey(objectKey);
        vo.setUploadUrl(uploadUrl);
        vo.setDownloadUrl(downloadUrl);
        vo.setExpiresIn(expiry);
        return vo;
    }

    @Override
    public List<FileVO> list(String bizType, Long bizId, String category) {
        List<SysFile> files = sysFileMapper.selectList(Wrappers.<SysFile>lambdaQuery()
                .eq(StringUtils.hasText(bizType), SysFile::getBizType, bizType)
                .eq(bizId != null, SysFile::getBizId, bizId)
                .eq(StringUtils.hasText(category), SysFile::getCategory, category)
                .orderByDesc(SysFile::getId));
        List<FileVO> result = new ArrayList<>();
        for (SysFile file : files) {
            FileVO vo = toVO(file);
            // 每次查询刷新访问地址,避免预签名过期
            try {
                vo.setUrl(fileStorage.presignedDownloadUrl(file.getObjectKey(), null));
            } catch (Exception ex) {
                log.debug("刷新访问地址失败:{}", file.getObjectKey());
            }
            result.add(vo);
        }
        return result;
    }

    @Override
    public FileVO downloadUrl(Long id) {
        SysFile file = sysFileMapper.selectById(id);
        if (file == null) {
            throw new BusinessException("文件不存在");
        }
        FileVO vo = toVO(file);
        vo.setUrl(fileStorage.presignedDownloadUrl(file.getObjectKey(), null));
        return vo;
    }

    @Override
    public void delete(Long id) {
        SysFile file = sysFileMapper.selectById(id);
        if (file == null) {
            return;
        }
        fileStorage.delete(file.getObjectKey());
        sysFileMapper.deleteById(id);
        log.info("资源已删除:id={}, key={}", id, file.getObjectKey());
    }

    @Override
    public String storageType() {
        return fileStorage.type();
    }

    /**
     * 对象键规则:业务类型/年月/随机文件名,避免同名覆盖与热点目录。
     */
    private String buildObjectKey(String bizType, String fileName) {
        String biz = StringUtils.hasText(bizType) ? bizType.toLowerCase() : "common";
        String month = LocalDate.now().format(MONTH_FORMAT);
        String ext = "";
        if (fileName != null && fileName.lastIndexOf('.') >= 0) {
            ext = fileName.substring(fileName.lastIndexOf('.')).toLowerCase();
        }
        return biz + "/" + month + "/" + UUID.randomUUID().toString().replace("-", "") + ext;
    }

    private SysFile saveRecord(String objectKey, String originalName, String contentType, long size,
                               String bizType, Long bizId, Long userId, String url) {
        SysFile entity = new SysFile();
        // 以实际生效的实现为准(可能已从 minio 降级为 local)
        entity.setBucket("minio".equals(fileStorage.type()) ? storageProperties.getMinio().getBucket() : "local");
        entity.setObjectKey(objectKey);
        entity.setOriginalName(originalName);
        entity.setContentType(contentType);
        entity.setSize(size);
        entity.setCategory(inferCategory(contentType, originalName));
        entity.setBizType(bizType);
        entity.setBizId(bizId);
        entity.setUrl(url);
        entity.setUploaderId(userId);
        sysFileMapper.insert(entity);
        return entity;
    }

    private String inferCategory(String contentType, String fileName) {
        String type = contentType == null ? "" : contentType.toLowerCase();
        String name = fileName == null ? "" : fileName.toLowerCase();
        if (type.startsWith("video/") || name.matches(".*\\.(mp4|avi|mov|mkv|flv|wmv|webm)$")) {
            return "VIDEO";
        }
        if (type.startsWith("image/") || name.matches(".*\\.(png|jpg|jpeg|gif|bmp|webp|svg)$")) {
            return "IMAGE";
        }
        if (type.startsWith("text/") || name.matches(".*\\.(pdf|doc|docx|ppt|pptx|xls|xlsx|txt|md)$")) {
            return "DOC";
        }
        return "OTHER";
    }

    private FileVO toVO(SysFile entity) {
        FileVO vo = new FileVO();
        BeanUtils.copyProperties(entity, vo);
        if (entity.getSize() != null) {
            vo.setSizeText(readableSize(entity.getSize()));
        }
        return vo;
    }

    private String readableSize(long size) {
        if (size <= 0) {
            return "0 B";
        }
        String[] units = {"B", "KB", "MB", "GB"};
        int index = 0;
        double value = size;
        while (value >= 1024 && index < units.length - 1) {
            value /= 1024;
            index++;
        }
        return String.format("%.1f %s", value, units[index]);
    }
}
