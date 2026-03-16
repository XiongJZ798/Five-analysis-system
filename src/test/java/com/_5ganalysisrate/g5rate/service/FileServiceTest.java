package com._5ganalysisrate.g5rate.service;

import com._5ganalysisrate.g5rate.dto.ApiResponse;
import com._5ganalysisrate.g5rate.model.TestData;
import com._5ganalysisrate.g5rate.repository.TestDataRepository;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.apache.poi.ss.usermodel.DateUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.Date;
import java.util.List;
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

    @Test
    void testProcessValidExcelFile() throws Exception {
        // 创建模拟的MultipartFile
        MultipartFile mockFile = mock(MultipartFile.class);
        when(mockFile.getOriginalFilename()).thenReturn("test.xlsx");
        
        InputStream inputStream = mock(InputStream.class);
        when(mockFile.getInputStream()).thenReturn(inputStream);
        
        Workbook mockWorkbook = mock(Workbook.class);
        Sheet mockSheet = mock(Sheet.class);
        Row dataRow = mock(Row.class);
        
        try (MockedStatic<WorkbookFactory> workbookFactoryMock = mockStatic(WorkbookFactory.class)) {
            workbookFactoryMock.when(() -> WorkbookFactory.create(inputStream)).thenReturn(mockWorkbook);
            
            when(mockWorkbook.getSheetAt(0)).thenReturn(mockSheet);
            when(mockSheet.getRow(1)).thenReturn(dataRow);
            when(mockSheet.getLastRowNum()).thenReturn(1);
            when(mockSheet.getPhysicalNumberOfRows()).thenReturn(2);
            
            // 模拟单元格数据（每列使用合法值）
            double[] columnValues = {
                DateUtil.getExcelDate(new Date()), // col0: TestTime as Excel serial
                -85.0,   // col1: RSRP
                15.0,    // col2: SINR
                100.0,   // col3: MacThroughput
                2.0,     // col4: Rank (1-8)
                15.0,    // col5: MCS (0-28)
                50.0     // col6: PRB
            };
            for (int i = 0; i < 7; i++) {
                Cell dataCell = mock(Cell.class);
                when(dataRow.getCell(i)).thenReturn(dataCell);
                when(dataCell.getCellType()).thenReturn(CellType.NUMERIC);
                when(dataCell.getNumericCellValue()).thenReturn(columnValues[i]);
            }
            
            // 预期保存的实体
            ArgumentCaptor<List<TestData>> testDataCaptor = ArgumentCaptor.forClass(List.class);
            
            // 执行测试
            ApiResponse<?> response = fileService.processExcelFile(mockFile);
            
            // 验证保存调用
            verify(testDataRepository).saveAll(testDataCaptor.capture());
            
            // 验证解析结果
            assertNotNull(response);
            assertEquals(200, response.getCode());
            
            // 验证解析的数据
            List<TestData> capturedData = testDataCaptor.getValue();
            assertFalse(capturedData.isEmpty());
        }
    }

    @Test
    void testHandleDatabaseError() throws Exception {
        MultipartFile mockFile = mock(MultipartFile.class);
        when(mockFile.getOriginalFilename()).thenReturn("test.xlsx");
        
        InputStream inputStream = mock(InputStream.class);
        when(mockFile.getInputStream()).thenReturn(inputStream);
        
        Workbook mockWorkbook = mock(Workbook.class);
        Sheet mockSheet = mock(Sheet.class);
        Row dataRow = mock(Row.class);
        
        try (MockedStatic<WorkbookFactory> workbookFactoryMock = mockStatic(WorkbookFactory.class)) {
            workbookFactoryMock.when(() -> WorkbookFactory.create(inputStream)).thenReturn(mockWorkbook);
            
            when(mockWorkbook.getSheetAt(0)).thenReturn(mockSheet);
            when(mockSheet.getRow(1)).thenReturn(dataRow);
            when(mockSheet.getLastRowNum()).thenReturn(1);
            when(mockSheet.getPhysicalNumberOfRows()).thenReturn(2);
            
            double[] columnValues = {
                DateUtil.getExcelDate(new Date()),
                -85.0, 15.0, 100.0, 2.0, 15.0, 50.0
            };
            for (int i = 0; i < 7; i++) {
                Cell dataCell = mock(Cell.class);
                when(dataRow.getCell(i)).thenReturn(dataCell);
                when(dataCell.getCellType()).thenReturn(CellType.NUMERIC);
                when(dataCell.getNumericCellValue()).thenReturn(columnValues[i]);
            }
            
            doThrow(new RuntimeException("Database error")).when(testDataRepository).saveAll(any());
            
            ApiResponse<?> response = fileService.processExcelFile(mockFile);
            
            assertNotNull(response);
            assertTrue(response.getCode() >= 400);
            assertTrue(response.getMessage().contains("文件处理失败"));
        }
    }
} 