package com.example.user.controller;

import com.example.common.domain.Result;
import com.example.common.utils.UserContext;
import com.example.user.permission.PermissionName;
import com.example.user.service.StorageService;
import com.example.user.vo.ImageUploadVO;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * 文件（图片）上传接口。
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/userservice/user")
public class FileController {

    private final StorageService storageService;

    @PreAuthorize("hasAuthority('user:avatar:update:me')")
    @PermissionName("上传头像")
    @PostMapping("/avatar/upload")
    public Result<ImageUploadVO> uploadAvatar(@RequestParam("file") MultipartFile file) {
        Long userId = UserContext.get().userId();
        String url = storageService.uploadImage(file, userId);
        return Result.success("上传成功", new ImageUploadVO(url));
    }
}
