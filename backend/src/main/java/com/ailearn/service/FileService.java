package com.ailearn.service;

import com.ailearn.vo.FileVO;
import com.ailearn.vo.UploadInfoVO;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * 学习资源管理(视频、图片、文档)。
 */
public interface FileService {

    /** 服务端中转上传 */
    FileVO upload(MultipartFile file, String bizType, Long bizId, Long userId);

    /** 生成预签名直传地址(前端直传对象存储) */
    UploadInfoVO presign(String fileName, String contentType, String bizType, Long bizId, Long userId);

    /** 查询资源列表 */
    List<FileVO> list(String bizType, Long bizId, String category);

    /** 刷新访问地址(预签名会过期) */
    FileVO downloadUrl(Long id);

    /** 删除资源(同时删除对象) */
    void delete(Long id);

    /** 对象存储实现类型 */
    String storageType();
}
