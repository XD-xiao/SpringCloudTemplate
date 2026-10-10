package com.example.user.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 图片上传结果。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ImageUploadVO {

    /** 图片公开访问 URL。 */
    private String url;
}
