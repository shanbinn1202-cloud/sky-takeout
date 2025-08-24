package com.sky.controller.admin;


import com.sky.constant.MessageConstant;
import com.sky.properties.AliOssProperties;
import com.sky.result.Result;
import com.sky.utils.AliOssUtil;
import io.swagger.annotations.Api;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.Message;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

@RestController
@RequestMapping("/admin/common")
@Api("common controller")
@Slf4j
public class CommonController {

    @Autowired
    private AliOssUtil aliOssUtil;

    @PostMapping("/upload")
    public Result<String> upload(MultipartFile file){
        try {
            log.info("file upload:{}", file);
            String filename = file.getOriginalFilename();
            String extention = filename.substring(filename.lastIndexOf('.'));
            String uuid = UUID.randomUUID().toString();
            String newname = uuid + extention;
            String filePath =  aliOssUtil.upload(file.getBytes(), newname);
            return Result.success(filePath);
        }catch (Exception e){
            log.info("upload error:{}",e);
        }
        return  Result.error(MessageConstant.UPLOAD_FAILED);

    }
}
