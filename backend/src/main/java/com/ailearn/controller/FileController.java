package com.ailearn.controller;

import com.ailearn.common.BusinessException;
import com.ailearn.common.Result;
import com.ailearn.common.UserContext;
import com.ailearn.config.StorageProperties;
import com.ailearn.security.AdminOnly;
import com.ailearn.service.FileService;
import com.ailearn.storage.FileStorage;
import com.ailearn.storage.LocalFileStorage;
import com.ailearn.vo.FileVO;
import com.ailearn.vo.UploadInfoVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * 学习资源接口(视频、图片、文档)。
 *
 * <p>两种上传方式:服务端中转(最简单)与预签名直传(前端直接 PUT 到对象存储,不占后端带宽)。</p>
 */
@RestController
@RequestMapping("/api/files")
@RequiredArgsConstructor
@Tag(name = "资源管理", description = "MinIO 对象存储 / 本地存储")
@Slf4j
public class FileController {

    private final FileService fileService;
    private final FileStorage fileStorage;
    private final StorageProperties storageProperties;

    @GetMapping("/storage")
    @Operation(summary = "当前对象存储实现")
    public Result<String> storage() {
        return Result.success(fileService.storageType());
    }

    @PostMapping("/upload")
    @Operation(summary = "上传资源(服务端中转)")
    public Result<FileVO> upload(@RequestParam("file") MultipartFile file,
                                 @RequestParam(required = false) String bizType,
                                 @RequestParam(required = false) Long bizId) {
        return Result.success(fileService.upload(file, bizType, bizId, UserContext.getUserId()));
    }

    @PostMapping("/presign")
    @Operation(summary = "获取预签名直传地址(前端直接 PUT 到对象存储)")
    public Result<UploadInfoVO> presign(@RequestParam String fileName,
                                        @RequestParam(required = false) String contentType,
                                        @RequestParam(required = false) String bizType,
                                        @RequestParam(required = false) Long bizId) {
        return Result.success(fileService.presign(fileName, contentType, bizType, bizId, UserContext.getUserId()));
    }

    @GetMapping
    @Operation(summary = "资源列表(按业务类型/业务 id/分类)")
    public Result<List<FileVO>> list(@RequestParam(required = false) String bizType,
                                     @RequestParam(required = false) Long bizId,
                                     @RequestParam(required = false) String category) {
        return Result.success(fileService.list(bizType, bizId, category));
    }

    @GetMapping("/{id}/url")
    @Operation(summary = "刷新资源访问地址(预签名会过期)")
    public Result<FileVO> url(@PathVariable Long id) {
        return Result.success(fileService.downloadUrl(id));
    }

    @DeleteMapping("/{id}")
    @AdminOnly
    @Operation(summary = "删除资源(管理员)")
    public Result<Void> delete(@PathVariable Long id) {
        fileService.delete(id);
        return Result.success();
    }

    /* ---------------- 本地存储模式下的签名直链(公开但校验签名与过期时间) ---------------- */

    @GetMapping("/local/download")
    @Operation(summary = "本地存储下载")
    public ResponseEntity<InputStreamResource> localDownload(@RequestParam String key,
                                                             @RequestParam long exp,
                                                             @RequestParam String sig) {
        LocalFileStorage storage = requireLocalStorage();
        storage.verify("download", key, exp, sig);
        InputStream in = storage.download(key);
        String fileName = key.substring(key.lastIndexOf('/') + 1);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "inline;filename*=UTF-8''" + URLEncoder.encode(fileName, StandardCharsets.UTF_8))
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(new InputStreamResource(in));
    }

    @PostMapping("/local/upload")
    @Operation(summary = "本地存储上传")
    public Result<String> localUpload(@RequestParam String key,
                                      @RequestParam long exp,
                                      @RequestParam String sig,
                                      @RequestParam("file") MultipartFile file) {
        LocalFileStorage storage = requireLocalStorage();
        storage.verify("upload", key, exp, sig);
        try {
            storage.upload(key, file.getInputStream(), file.getSize(), file.getContentType());
            return Result.success(storage.presignedDownloadUrl(key, null));
        } catch (Exception ex) {
            throw new BusinessException("上传失败:" + ex.getMessage());
        }
    }

    /**
     * 本地签名直链只在实际生效的实现是本地存储时可用。
     * 注意:必须判断真实 Bean 而不是配置项——配置为 minio 但 MinIO 不可用时会自动降级为本地存储。
     */
    private LocalFileStorage requireLocalStorage() {
        if (fileStorage instanceof LocalFileStorage local) {
            return local;
        }
        throw new BusinessException("当前对象存储为 " + fileStorage.type() + ",请使用预签名地址访问");
    }
}
