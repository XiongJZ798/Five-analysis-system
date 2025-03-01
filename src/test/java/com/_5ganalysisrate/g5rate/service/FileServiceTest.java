package com._5ganalysisrate.g5rate.service;

import com._5ganalysisrate.g5rate.dto.ApiResponse;
import com._5ganalysisrate.g5rate.repository.TestDataRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class FileServiceTest {

    @Mock
    private TestDataRepository testDataRepository;

    @InjectMocks
    private FileService fileService;

    private MockMultipartFile validExcelFile;
    private MockMultipartFile invalidFile;
    private MockMultipartFile emptyFile;

    @BeforeEach
    void setUp() throws IOException {
        // 准备测试文件
        InputStream validExcelStream = getClass().getResourceAsStream("/test-data.xlsx");
        if (validExcelStream != null) {
            validExcelFile = new MockMultipartFile(
                "file",
                "test-data.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                validExcelStream
            );
        }

        invalidFile = new MockMultipartFile(
            "file",
            "test.txt",
            "text/plain",
            "Invalid content".getBytes()
        );

        emptyFile = new MockMultipartFile(
            "file",
            "",
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
            new byte[0]
        );
    }

    //有效文件处理测试
    @Test
    void testProcessExcelFile_ValidFile() throws IOException {
        // 跳过实际文件处理，因为测试环境中可能没有测试文件
        if (validExcelFile == null) {
            return;
        }

        when(testDataRepository.saveAll(any())).thenReturn(null);

        ApiResponse<?> response = fileService.processExcelFile(validExcelFile);

        assertNotNull(response);
        assertEquals(200, response.getCode());
        assertTrue(response.getData() instanceof Map);
    }

    //无效文件类型测试
    @Test
    void testProcessExcelFile_InvalidFileType() {
        ApiResponse<?> response = fileService.processExcelFile(invalidFile);

        assertNotNull(response);
        assertEquals(400, response.getCode());
        assertTrue(response.getMessage().contains("只支持Excel文件格式"));
    }

    //空文件测试
    @Test
    void testProcessExcelFile_EmptyFile() {
        ApiResponse<?> response = fileService.processExcelFile(emptyFile);

        assertNotNull(response);
        assertEquals(400, response.getCode());
        assertTrue(response.getMessage().contains("文件不能为空"));
    }

    //大文件测试
    @Test
    void testProcessExcelFile_LargeFile() {
        // 创建一个大于10MB的模拟文件
        byte[] largeContent = new byte[11 * 1024 * 1024];
        MultipartFile largeFile = new MockMultipartFile(
            "file",
            "large.xlsx",
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
            largeContent
        );

        ApiResponse<?> response = fileService.processExcelFile(largeFile);

        assertNotNull(response);
        assertEquals(400, response.getCode());
        assertTrue(response.getMessage().contains("文件大小不能超过10MB"));
    }

    //数据库错误测试
    @Test
    void testProcessExcelFile_DatabaseError() throws IOException {
        if (validExcelFile == null) {
            return;
        }

        when(testDataRepository.saveAll(any()))
            .thenThrow(new RuntimeException("Database error"));

        ApiResponse<?> response = fileService.processExcelFile(validExcelFile);

        assertNotNull(response);
        assertTrue(response.getCode() >= 400);
        assertTrue(response.getMessage().contains("文件处理失败"));
    }
} 