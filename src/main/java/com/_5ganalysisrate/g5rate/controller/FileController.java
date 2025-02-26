package com._5ganalysisrate.g5rate.controller;

import com._5ganalysisrate.g5rate.dto.ApiResponse;
import com._5ganalysisrate.g5rate.service.FileService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/file")
@CrossOrigin(origins = "http://localhost:3000", allowCredentials = "true")
public class FileController {

    @Autowired
    private FileService fileService;

    @PostMapping("/upload")
    public ApiResponse<?> uploadFile(@RequestParam("file") MultipartFile file) {
        try {
            return fileService.processExcelFile(file);
        } catch (Exception e) {
            e.printStackTrace();
            return ApiResponse.error(e.getMessage());
        }
    }
} 