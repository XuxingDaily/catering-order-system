package com.catering.infrastructure.storage.controller;

import com.catering.shared.constant.MessageConstant;
import com.catering.shared.result.Result;
import com.catering.utils.AliOssUtil;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.Objects;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/admin/common")
@Api(tags = "公共接口")
public class CommonController {

    @Autowired
    private AliOssUtil aliOssUtil;

    @PostMapping("/upload")
    @ApiOperation("文件上传")
    public Result<String> uploadImage(MultipartFile file) {
        log.info("文件上传：{}", file.getOriginalFilename());

        try {
            String oldFileName = file.getOriginalFilename();
            String extension = Objects.requireNonNull(oldFileName).substring(file.getOriginalFilename().lastIndexOf("."));
            String newFileName = UUID.randomUUID() + extension;

            String url = aliOssUtil.upload(file.getBytes(), newFileName);

            return Result.success(url);
        } catch (Exception e) {
            log.error("<UNK>", e);
            return Result.error(MessageConstant.UPLOAD_FAILED);
        }
    }

}
