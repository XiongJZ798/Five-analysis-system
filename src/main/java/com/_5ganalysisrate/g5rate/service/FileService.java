package com._5ganalysisrate.g5rate.service;

import com._5ganalysisrate.g5rate.dto.ApiResponse;
import com._5ganalysisrate.g5rate.model.TestData;
import com._5ganalysisrate.g5rate.repository.TestDataRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class FileService {

    private final TestDataRepository testDataRepository;

    // 支持的日期时间格式
    private static final DateTimeFormatter[] DATE_FORMATTERS = {
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"),
        DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss"),
        DateTimeFormatter.ofPattern("yyyy.MM.dd HH:mm:ss"),
        DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss"),
        DateTimeFormatter.ofPattern("MM/dd/yyyy HH:mm:ss"),
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS"),
        DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss.SSS"),
        DateTimeFormatter.ofPattern("yyyy.MM.dd HH:mm:ss.SSS"),
        DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss.SSS"),
        DateTimeFormatter.ofPattern("MM/dd/yyyy HH:mm:ss.SSS"),
        // 添加更多格式支持
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"),
        DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm"),
        DateTimeFormatter.ofPattern("yyyy.MM.dd HH:mm"),
        DateTimeFormatter.ofPattern("yyyy-MM-dd"),
        DateTimeFormatter.ofPattern("yyyy/MM/dd"),
        DateTimeFormatter.ofPattern("yyyy.MM.dd"),
        DateTimeFormatter.ofPattern("HH:mm:ss"),
        DateTimeFormatter.ofPattern("HH:mm:ss.SSS")
    };

    public ApiResponse<?> processExcelFile(MultipartFile file) {
        if (file.isEmpty()) {
            return ApiResponse.error(400, "文件不能为空");
        }

        if (file.getSize() > 10 * 1024 * 1024) {
            return ApiResponse.error(400, "文件大小不能超过10MB");
        }

        String fileName = file.getOriginalFilename();
        if (fileName == null || (!fileName.endsWith(".xlsx") && !fileName.endsWith(".xls"))) {
            return ApiResponse.error(400, "只支持Excel文件格式(.xls/.xlsx)");
        }

        List<String> errorMessages = new ArrayList<>();
        List<TestData> dataList = new ArrayList<>();
        
        try (Workbook workbook = WorkbookFactory.create(file.getInputStream())) {
            Sheet sheet = workbook.getSheetAt(0);
            
            // 跳过表头行
            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null) {
                    log.debug("跳过空行: {}", i + 1);
                    continue;
                }

                try {
                    TestData data = processRow(row, i + 1, errorMessages);
                    if (data != null) {
                        dataList.add(data);
                    }
                } catch (Exception e) {
                    log.error("处理第{}行时发生错误: {}", i + 1, e.getMessage());
                    errorMessages.add(String.format("第%d行：数据处理错误 - %s", i + 1, e.getMessage()));
                }
            }

            if (dataList.isEmpty()) {
                return ApiResponse.error(400, "没有有效的数据可以导入。\n" + String.join("\n", errorMessages));
            }

            testDataRepository.saveAll(dataList);

            Map<String, Object> result = new HashMap<>();
            result.put("importCount", dataList.size());
            result.put("fileName", fileName);
            if (!errorMessages.isEmpty()) {
                result.put("warnings", errorMessages);
            }
            
            log.info("成功导入{}条数据，文件名：{}", dataList.size(), fileName);
            return ApiResponse.success(result);

        } catch (Exception e) {
            log.error("文件处理失败", e);
            String errorMsg = "文件处理失败: " + e.getMessage();
            if (!errorMessages.isEmpty()) {
                errorMsg += "\n详细错误：\n" + String.join("\n", errorMessages);
            }
            return ApiResponse.error(errorMsg);
        }
    }

    private TestData processRow(Row row, int rowNum, List<String> errorMessages) {
        TestData data = new TestData();
        
        try {
            // 获取并验证测试时间
            LocalDateTime testTime = getCellValueAsDateTime(row.getCell(0));
            if (testTime == null) {
                errorMessages.add(String.format("第%d行：测试时间不能为空", rowNum));
                return null;
            }
            data.setTestTime(testTime);

            // 获取并验证RSRP
            Double rsrp = getCellValueAsDouble(row.getCell(1));
            if (rsrp == null || rsrp < -140 || rsrp > -40) {
                errorMessages.add(String.format("第%d行：RSRP值无效（应在-140到-40之间）", rowNum));
                return null;
            }
            data.setRsrp(rsrp);

            // 获取并验证SINR
            Double sinr = getCellValueAsDouble(row.getCell(2));
            if (sinr == null || sinr < -20 || sinr > 30) {
                errorMessages.add(String.format("第%d行：SINR值无效（应在-20到30之间）", rowNum));
                return null;
            }
            data.setSinr(sinr);

            // 获取并验证MAC吞吐量
            Double macThroughput = getCellValueAsDouble(row.getCell(3));
            if (macThroughput == null || macThroughput < 0) {
                errorMessages.add(String.format("第%d行：MAC吞吐量无效（应大于0）", rowNum));
                return null;
            }
            data.setMacThroughput(macThroughput);

            // 获取并验证Rank
            Integer rank = getCellValueAsInteger(row.getCell(4));
            if (rank == null || rank < 1 || rank > 8) {
                errorMessages.add(String.format("第%d行：Rank值无效（应在1到8之间）", rowNum));
                return null;
            }
            data.setRank(rank);

            // 获取并验证MCS
            Integer mcs = getCellValueAsInteger(row.getCell(5));
            if (mcs == null || mcs < 0 || mcs > 28) {
                errorMessages.add(String.format("第%d行：MCS值无效（应在0到28之间）", rowNum));
                    return null;
                }
            data.setMcs(mcs);

            // 获取并验证RB数
            Integer rbNum = getCellValueAsInteger(row.getCell(6));
            if (rbNum == null || rbNum < 0) {
                errorMessages.add(String.format("第%d行：RB数无效（应大于0）", rowNum));
                return null;
            }
            data.setRbNum(rbNum);

            // 获取并验证BLER
            Double bler = getCellValueAsDouble(row.getCell(7));
            if (bler == null || bler < 0 || bler > 100) {
                errorMessages.add(String.format("第%d行：BLER值无效（应在0到100之间）", rowNum));
                return null;
            }
            data.setBler(bler);

            // 设置创建和更新时间
            LocalDateTime now = LocalDateTime.now();
            data.setCreateTime(now);
            data.setUpdateTime(now);

            return data;
        } catch (Exception e) {
            log.error("处理第{}行数据时发生错误", rowNum, e);
            errorMessages.add(String.format("第%d行：数据处理错误 - %s", rowNum, e.getMessage()));
            return null;
        }
    }

    private LocalDateTime getCellValueAsDateTime(Cell cell) {
        if (cell == null) {
            log.debug("日期时间单元格为空");
            return null;
        }
        
        try {
            switch (cell.getCellType()) {
                case NUMERIC:
                    if (DateUtil.isCellDateFormatted(cell)) {
                        try {
                            return cell.getLocalDateTimeCellValue();
                        } catch (Exception e) {
                            log.debug("无法直接获取LocalDateTime，尝试通过Date转换", e);
                            try {
                                return cell.getDateCellValue().toInstant()
                                        .atZone(java.time.ZoneId.systemDefault())
                                        .toLocalDateTime();
                            } catch (Exception e2) {
                                log.debug("通过Date转换失败，尝试其他方式", e2);
                            }
                        }
                    }
                    
                    // 尝试将数值作为Excel日期序列号处理
                    try {
                        double numericValue = cell.getNumericCellValue();
                        if (numericValue > 1) {  // Excel中的日期从1900年开始
                            return DateUtil.getJavaDate(numericValue)
                                .toInstant()
                                    .atZone(java.time.ZoneId.systemDefault())
                                .toLocalDateTime();
                        }
                    } catch (Exception e) {
                        log.debug("数值转换为日期失败", e);
                    }
                    break;

                case STRING:
                    String value = cell.getStringCellValue().trim();
                    if (!value.isEmpty()) {
                        // 首先尝试直接解析完整的日期时间
                        for (DateTimeFormatter formatter : DATE_FORMATTERS) {
                            try {
                                if (value.length() <= formatter.toString().length()) {
                                    if (formatter.toString().contains("HH:mm:ss") && !value.contains(":")) {
                                        continue;
                                    }
                                    LocalDateTime dateTime = LocalDateTime.parse(value, formatter);
                                    if (dateTime != null) {
                                        return dateTime;
                                    }
                                }
                            } catch (DateTimeParseException e) {
                                continue;
                            }
                        }

                    // 处理只有时间的情况
                        if (value.matches("\\d{1,2}:\\d{2}(:\\d{2})?(\\.*\\d*)?")) {
                        try {
                                String[] parts = value.split("[:.]");
                            int hour = Integer.parseInt(parts[0]);
                            int minute = Integer.parseInt(parts[1]);
                                int second = parts.length > 2 ? Integer.parseInt(parts[2]) : 0;
                                int nanos = parts.length > 3 ? Integer.parseInt(parts[3]) * 1_000_000 : 0;
                            
                            return LocalDateTime.now()
                                    .withHour(hour)
                                    .withMinute(minute)
                                    .withSecond(second)
                                        .withNano(nanos);
                        } catch (Exception e) {
                                log.debug("时间格式解析失败: {}", value, e);
                            }
                        }
                    }
                    break;

                case FORMULA:
                    try {
                        FormulaEvaluator evaluator = cell.getSheet().getWorkbook()
                                .getCreationHelper()
                                .createFormulaEvaluator();
                        CellValue cellValue = evaluator.evaluate(cell);
                        
                        Cell tempCell = cell.getRow().createCell(cell.getColumnIndex() + 100);
                        try {
                            switch (cellValue.getCellType()) {
                                case NUMERIC:
                                    tempCell.setCellValue(cellValue.getNumberValue());
                                    return getCellValueAsDateTime(tempCell);
                                case STRING:
                                    tempCell.setCellValue(cellValue.getStringValue());
                                    return getCellValueAsDateTime(tempCell);
                                default:
                                    break;
                            }
                        } finally {
                            cell.getRow().removeCell(tempCell);
                        }
                    } catch (Exception e) {
                        log.debug("公式计算失败", e);
                    }
                    break;
            }
            
            log.error("无法解析日期时间值: {}", cell);
            throw new IllegalArgumentException("无法解析日期时间值");
        } catch (Exception e) {
            log.error("解析日期时间值时发生错误: {}", cell, e);
            throw new IllegalArgumentException("日期时间格式错误: " + e.getMessage());
        }
    }

    private Double getCellValueAsDouble(Cell cell) {
        if (cell == null) {
            log.debug("数值单元格为空");
            return null;
        }
        
        try {
            switch (cell.getCellType()) {
                case NUMERIC:
                    return cell.getNumericCellValue();
                case STRING:
                    String value = cell.getStringCellValue().trim();
                    if (value.isEmpty()) {
                        return null;
                    }
                    return Double.parseDouble(value);
                case BLANK:
                    return null;
                default:
                    throw new IllegalArgumentException("单元格类型不支持转换为数值");
            }
        } catch (Exception e) {
            log.error("解析数值时发生错误", e);
            throw new IllegalArgumentException("数值格式错误: " + e.getMessage());
        }
    }

    private Integer getCellValueAsInteger(Cell cell) {
        if (cell == null) {
            log.debug("整数单元格为空");
            return null;
        }
        
        try {
            switch (cell.getCellType()) {
                case NUMERIC:
                    return (int) cell.getNumericCellValue();
                case STRING:
                    String value = cell.getStringCellValue().trim();
                    if (value.isEmpty()) {
                        return null;
                    }
                    return Integer.parseInt(value);
                case BLANK:
        return null;
                default:
                    throw new IllegalArgumentException("单元格类型不支持转换为整数");
            }
        } catch (Exception e) {
            log.error("解析整数时发生错误", e);
            throw new IllegalArgumentException("整数格式错误: " + e.getMessage());
        }
    }
} 