package com.itxindeshang.controller.uploadimage;

import com.itxindeshang.common.result.Result;
import com.itxindeshang.util.AliyunOSSUtils;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RequestMapping("/api")
@RestController
@Slf4j
public class UploadImageController {
    @Resource
    private AliyunOSSUtils aliyunOSSUtils;

    @PostMapping("/upload/image")
    public Result<String> upload(MultipartFile file) {
        log.info("图片上传: {}", file.getOriginalFilename());
        String url = aliyunOSSUtils.upload(file);
        return Result.success(url);
    }
}
