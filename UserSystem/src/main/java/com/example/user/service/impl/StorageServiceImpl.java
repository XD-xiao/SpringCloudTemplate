package com.example.user.service.impl;

import com.example.common.exception.BusinessException;
import com.example.user.config.RustFsProperties;
import com.example.user.service.StorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.IOException;
import java.util.Set;
import java.util.UUID;

/**
 * {@link StorageService} 实现，基于 RustFS（S3 兼容）。
 */
@Service
@RequiredArgsConstructor
public class StorageServiceImpl implements StorageService {

    /** 允许的图片扩展名。 */
    private static final Set<String> ALLOWED_EXTENSIONS =
            Set.of("jpg", "jpeg", "png", "gif", "webp", "bmp");

    /** 单张图片最大 5MB。 */
    private static final long MAX_SIZE = 5 * 1024 * 1024L;

    private final S3Client s3Client;
    private final RustFsProperties props;

    @Override
    public String uploadImage(MultipartFile file, Long userId) {
        // 1. 基础校验
        if (file == null || file.isEmpty()) {
            throw new BusinessException(400, "上传图片不能为空");
        }
        if (file.getSize() > MAX_SIZE) {
            throw new BusinessException(400, "图片大小不能超过 5MB");
        }

        // 2. 扩展名校验
        String ext = StringUtils.getFilenameExtension(file.getOriginalFilename());
        if (ext == null || !ALLOWED_EXTENSIONS.contains(ext.toLowerCase())) {
            throw new BusinessException(400, "仅支持 jpg/jpeg/png/gif/webp/bmp 图片");
        }
        ext = ext.toLowerCase();

        // 3. 生成对象 Key：avatar/{userId}/{uuid}.{ext}
        String key = "avatar/" + userId + "/"
                + UUID.randomUUID().toString().replace("-", "") + "." + ext;

        // 4. 上传到 RustFS
        try {
            s3Client.putObject(
                    PutObjectRequest.builder()
                            .bucket(props.getBucket())
                            .key(key)
                            .contentType(resolveContentType(ext, file.getContentType()))
                            .build(),
                    RequestBody.fromInputStream(file.getInputStream(), file.getSize()));
        } catch (IOException e) {
            throw new BusinessException(500, "图片上传失败");
        }

        // 5. 返回公开访问 URL
        return buildPublicUrl(key);
    }

    /**
     * 拼接公开访问 URL。
     */
    private String buildPublicUrl(String key) {
        String base = props.getPublicBaseUrl();
        String s = (base != null && base.endsWith("/")) ? base : base + "/";
        return s + key;
    }

    /**
     * 解析 Content-Type：优先用前端提交的 image/* 类型，否则按扩展名推断。
     */
    private String resolveContentType(String ext, String contentType) {
        if (contentType != null && contentType.startsWith("image/")) {
            return contentType;
        }
        return switch (ext) {
            case "jpg", "jpeg" -> "image/jpeg";
            case "png" -> "image/png";
            case "gif" -> "image/gif";
            case "webp" -> "image/webp";
            case "bmp" -> "image/bmp";
            default -> "application/octet-stream";
        };
    }
}
