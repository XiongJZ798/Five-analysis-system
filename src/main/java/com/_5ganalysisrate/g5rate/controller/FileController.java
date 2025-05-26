package com._5ganalysisrate.g5rate.controller;

import com._5ganalysisrate.g5rate.dto.ApiResponse;
import com._5ganalysisrate.g5rate.service.FileService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@RestController
@RequestMapping("/file")
@CrossOrigin(originPatterns = {"http://localhost:*", "http://127.0.0.1:*"}, allowCredentials = "true", maxAge = 3600)
public class FileController {

    @Autowired
    private FileService fileService;

    @PostMapping("/upload")
    public ApiResponse<?> uploadFile(@RequestParam("file") MultipartFile file) {
        log.info("接收到文件上传请求: fileName={}, fileSize={}", 
            file.getOriginalFilename(), file.getSize());
            
        try {
            ApiResponse<?> response = fileService.processExcelFile(file);
            log.info("文件处理完成: fileName={}, response={}", 
                file.getOriginalFilename(), response);
            return response;
        } catch (Exception e) {
            log.error("文件处理失败: fileName=" + file.getOriginalFilename(), e);
            return ApiResponse.error(e.getMessage());
        }
    }
} 