package com.example.user.service;

import org.springframework.web.multipart.MultipartFile;

/**
 * 对象存储业务接口。
 */
public interface StorageService {

    /**
     * 上传一张图片，返回可公开访问的 URL。
     *
     * @param file   图片文件
     * @param userId 当前用户 ID（用于组织对象 Key）
     * @return 图片公开访问 URL
     */
    String uploadImage(MultipartFile file, Long userId);
}
