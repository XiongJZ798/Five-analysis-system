package com._5ganalysisrate.g5rate.controller;

import com._5ganalysisrate.g5rate.dto.ApiResponse;
import com._5ganalysisrate.g5rate.dto.AnalysisTaskResponse;
import com._5ganalysisrate.g5rate.dto.CreateAnalysisTaskRequest;
import com._5ganalysisrate.g5rate.domain.enums.TaskType;
import com._5ganalysisrate.g5rate.service.AnalysisTaskService;
import com._5ganalysisrate.g5rate.service.FileService;
import com._5ganalysisrate.g5rate.service.ReportService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/file")
@CrossOrigin(originPatterns = {"http://localhost:*", "http://127.0.0.1:*"}, allowCredentials = "true", maxAge = 3600)
public class FileController {

    @Autowired
    private FileService fileService;

    @Autowired
    private AnalysisTaskService analysisTaskService;

    @Autowired
    private ReportService reportService;

    @PostMapping("/upload")
    public ApiResponse<?> uploadFile(@RequestParam("file") MultipartFile file,
                                      @RequestParam(value = "taskType", required = false) String taskType) {
        log.info("接收到文件上传请求: fileName={}, fileSize={}", 
            file.getOriginalFilename(), file.getSize());
            
        try {
            ApiResponse<?> response = fileService.processExcelFile(file);
            log.info("文件处理完成: fileName={}", file.getOriginalFilename());

            // 文件上传成功后，自动触发分析任务
            if (response.getCode() == 200) {
                try {
                    String fileId = file.getOriginalFilename();
                    TaskType type = "RULE_ONLY".equalsIgnoreCase(taskType) ? TaskType.RULE_ONLY : TaskType.RULE_AI;

                    CreateAnalysisTaskRequest taskRequest = new CreateAnalysisTaskRequest();
                    taskRequest.setFileId(fileId);
                    taskRequest.setTaskType(type);

                    AnalysisTaskResponse taskResponse = reportService.buildTaskResponse(
                            analysisTaskService.createTask(taskRequest));

                    // 将任务信息附加到响应数据中
                    Map<String, Object> enrichedData = new HashMap<>();
                    enrichedData.put("importResult", response.getData());
                    enrichedData.put("analysisTask", taskResponse);

                    log.info("分析任务已自动创建, taskId={}, type={}", taskResponse.getTaskId(), type);
                    return ApiResponse.success(enrichedData);
                } catch (Exception taskEx) {
                    // 分析任务创建失败不影响文件导入结果
                    log.warn("文件上传成功，但自动创建分析任务失败: {}", taskEx.getMessage());
                }
            }

            return response;
        } catch (Exception e) {
            log.error("文件处理失败: fileName=" + file.getOriginalFilename(), e);
            return ApiResponse.error(e.getMessage());
        }
    }
} 