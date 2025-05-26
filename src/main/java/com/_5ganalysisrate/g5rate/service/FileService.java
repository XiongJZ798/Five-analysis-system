package com._5ganalysisrate.g5rate.service;

import com._5ganalysisrate.g5rate.dto.ApiResponse;
import com._5ganalysisrate.g5rate.model.TestData;
import com._5ganalysisrate.g5rate.repository.TestDataRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

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
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS"),
        DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss.SSS"),
        DateTimeFormatter.ofPattern("yyyy.MM.dd HH:mm:ss.SSS"),
        DateTimeFormatter.ofPattern("HH:mm:ss.SSS"),
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"),
        DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss"),
        DateTimeFormatter.ofPattern("yyyy.MM.dd HH:mm:ss"),
        DateTimeFormatter.ofPattern("HH:mm:ss"),
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"),
        DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm"),
        DateTimeFormatter.ofPattern("yyyy-MM-dd"),
        DateTimeFormatter.ofPattern("yyyy/MM/dd"),
        DateTimeFormatter.ISO_LOCAL_DATE_TIME
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
            
            // 验证Excel文件结构
            ApiResponse<?> validationResponse = validateExcelStructure(sheet);
            if (validationResponse.getCode() != 200) {
                return validationResponse;
            }

            int validRowCount = 0; // 有效行计数器
            int lastRowNum = sheet.getLastRowNum();
            boolean foundSummaryRow = false; // 是否找到了总结/平均行
            
            log.info("开始处理Excel文件，总行数: {}", lastRowNum);
            
            // 记录平均值行索引，默认为-1表示没有找到
            int averageRowIndex = -1;
            
            // 尝试检测最后一行是否是平均值行（通常带有"平均"或使用特殊格式）
            Row lastRow = sheet.getRow(lastRowNum);
            if (lastRow != null) {
                Cell firstCell = lastRow.getCell(0);
                if (firstCell != null) {
                    String cellValue = "";
                    if (firstCell.getCellType() == CellType.STRING) {
                        cellValue = firstCell.getStringCellValue();
                        log.info("最后一行首列内容: {}", cellValue);
                    } else if (firstCell.getCellType() == CellType.NUMERIC) {
                        // 检查是否是数值而不是时间格式
                        if (!DateUtil.isCellDateFormatted(firstCell)) {
                            double value = firstCell.getNumericCellValue();
                            log.info("最后一行首列是数值: {}", value);
                            
                            // 如果前几行都是时间格式但最后一行是数值，很可能是平均值行
                            for (int i = lastRowNum - 3; i <= lastRowNum - 1; i++) {
                                if (i < 1) continue;
                                Row checkRow = sheet.getRow(i);
                                if (checkRow != null) {
                                    Cell checkCell = checkRow.getCell(0);
                                    if (checkCell != null && DateUtil.isCellDateFormatted(checkCell)) {
                                        averageRowIndex = lastRowNum;
                                        foundSummaryRow = true;
                                        log.info("检测到第{}行可能是平均值/汇总行（基于格式差异判断）", lastRowNum + 1);
                                        break;
                                    }
                                }
                            }
                        }
                    }
                    
                    // 通过文本内容判断是否为平均值行
                    if (cellValue.contains("平均") || cellValue.contains("average") || 
                        cellValue.contains("avg") || cellValue.contains("mean") ||
                        cellValue.contains("汇总") || cellValue.contains("summary") ||
                        cellValue.contains("total")) {
                        averageRowIndex = lastRowNum;
                        foundSummaryRow = true;
                        log.info("检测到第{}行是平均值/汇总行", lastRowNum + 1);
                    }
                }
            }
            
            // 增强平均值行检测：检查额外的数值特征
            // 有时平均值行的首单元格可能不是明确的文本标记，但其格式和内容与其他行明显不同
            if (!foundSummaryRow && lastRowNum > 10) {
                // 检查最后一行的时间格式
                Row lastDataRow = sheet.getRow(lastRowNum);
                if (lastDataRow != null) {
                    Cell timeCell = lastDataRow.getCell(0);
                    
                    // 如果最后一行首列不是日期格式，但前面的行都是，这可能是平均值行
                    if (timeCell != null && timeCell.getCellType() == CellType.NUMERIC) {
                        // 如果数值是小数（例如-80.538），而前面行的数值都是整数格式的时间，很可能是平均值
                        double cellValue = timeCell.getNumericCellValue();
                        boolean isTimeFormat = DateUtil.isCellDateFormatted(timeCell);
                        
                        // 如果不是时间格式，检查是否是负数（如RSRP平均值）
                        if (!isTimeFormat && cellValue < 0) {
                            log.info("最后一行首列是负数({}),可能是RSRP平均值行", cellValue);
                            averageRowIndex = lastRowNum;
                            foundSummaryRow = true;
                        }
                        
                        // 检查前几行是否都是时间格式
                        if (!foundSummaryRow) {
                            boolean previousAreTime = true;
                            for (int i = Math.max(1, lastRowNum - 5); i < lastRowNum; i++) {
                                Row prevRow = sheet.getRow(i);
                                if (prevRow != null) {
                                    Cell prevTimeCell = prevRow.getCell(0);
                                    if (prevTimeCell == null || !DateUtil.isCellDateFormatted(prevTimeCell)) {
                                        previousAreTime = false;
                                        break;
                                    }
                                }
                            }
                            
                            if (previousAreTime && !isTimeFormat) {
                                log.info("最后一行首列格式与前几行不同,可能是平均值行");
                                averageRowIndex = lastRowNum;
                                foundSummaryRow = true;
                            }
                        }
                    }
                }
            }
            
            // 实际行检查计数器，包括所有尝试处理的行
            int actualRowCount = 0;
            
            for (int i = 1; i <= lastRowNum; i++) {
                // 跳过平均值行
                if (i == averageRowIndex) {
                    log.info("跳过平均值/汇总行: {}", i + 1);
                    continue;
                }
                
                Row row = sheet.getRow(i);
                if (row == null) {
                    log.debug("跳过空行: {}", i + 1);
                    continue;
                }

                // 改进空行检测逻辑：检查关键列是否都为有效值
                boolean isValidRow = true;
                // 检查时间列、RSRP列和SINR列是否为有效数据
                Cell timeCell = row.getCell(0);
                Cell rsrpCell = row.getCell(1);
                Cell sinrCell = row.getCell(2);
                Cell macThrCell = row.getCell(3);
                
                // 时间列必须是有效的日期时间格式
                if (timeCell == null || 
                    (timeCell.getCellType() != CellType.NUMERIC && timeCell.getCellType() != CellType.STRING) ||
                    getCellValueAsDateTime(timeCell) == null) {
                    log.debug("第{}行时间列无效", i + 1);
                    isValidRow = false;
                }
                
                // RSRP和SINR必须是有效的数值
                if (rsrpCell == null || sinrCell == null || macThrCell == null ||
                    getCellValueAsDouble(rsrpCell) == null || 
                    getCellValueAsDouble(sinrCell) == null ||
                    getCellValueAsDouble(macThrCell) == null) {
                    log.debug("第{}行RSRP/SINR/吞吐量列无效", i + 1);
                    isValidRow = false;
                }
                
                // 检查吞吐量是否为正值
                Double macThr = getCellValueAsDouble(macThrCell);
                if (macThr != null && macThr <= 0) {
                    log.debug("第{}行吞吐量不是正值: {}", i + 1, macThr);
                    isValidRow = false;
                }

                if (!isValidRow) {
                    log.info("跳过无效数据行: {}", i + 1);
                    continue;
                }

                actualRowCount++; // 增加实际处理的行计数
                validRowCount++; // 增加有效行计数

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

            log.info("Excel文件共有{}行有效数据，成功处理{}行，最终导入{}行", 
                    validRowCount, actualRowCount, dataList.size());

            if (dataList.isEmpty()) {
                return ApiResponse.error(400, "没有有效的数据可以导入。\n" + String.join("\n", errorMessages));
            }

            // 在保存新数据前先清除所有旧数据
            log.info("清除所有旧测试数据，准备导入新数据");
            testDataRepository.deleteAll();
            
            // 重置自增ID值
            try {
                log.info("尝试重置自增ID值");
                testDataRepository.resetAutoIncrement();
            } catch (Exception e) {
                log.warn("重置自增ID失败: {}", e.getMessage());
            }

            // 保存新数据
            testDataRepository.saveAll(dataList);

            Map<String, Object> result = new HashMap<>();
            result.put("importCount", dataList.size());
            result.put("totalRows", lastRowNum);
            result.put("skippedRows", lastRowNum - dataList.size());
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
            // 获取并验证测试时间，确保精确到毫秒
            LocalDateTime testTime = getCellValueAsDateTime(row.getCell(0));
            if (testTime == null) {
                errorMessages.add(String.format("第%d行：测试时间不能为空", rowNum));
                return null;
            }
            data.setTestTime(testTime);

            // 获取并验证RSRP，5G范围：-140dBm到-44dBm
            Double rsrp = getCellValueAsDouble(row.getCell(1));
            if (rsrp == null) {
                errorMessages.add(String.format("第%d行：RSRP值不能为空", rowNum));
                return null;
            }
            // 验证但允许超出正常范围的值
            if (rsrp < -140 || rsrp > -44) {
                log.warn("第{}行：RSRP值{}超出正常范围(-140到-44dBm)", rowNum, rsrp);
            }
            data.setRsrp(rsrp);

            // 获取并验证SINR，5G范围：-20dB到40dB
            Double sinr = getCellValueAsDouble(row.getCell(2));
            if (sinr == null) {
                errorMessages.add(String.format("第%d行：SINR值不能为空", rowNum));
                return null;
            }
            // 验证但允许超出正常范围的值
            if (sinr < -20 || sinr > 40) {
                log.warn("第{}行：SINR值{}超出正常范围(-20到40dB)", rowNum, sinr);
            }
            data.setSinr(sinr);

            // 获取并验证MAC吞吐量，不设上限
            Double macThroughput = getCellValueAsDouble(row.getCell(3));
            if (macThroughput == null || macThroughput < 0) {
                errorMessages.add(String.format("第%d行：MAC吞吐量无效（应大于0）", rowNum));
                return null;
            }
            data.setMacThroughput(macThroughput);

            // 获取并验证Rank，范围：1-8
            Integer rank;
            try {
                rank = getCellValueAsInteger(row.getCell(4));
            } catch (Exception e) {
                // 如果直接获取整数失败，尝试先获取double然后转换
                try {
                    Double doubleRank = getCellValueAsDouble(row.getCell(4));
                    if (doubleRank != null) {
                        rank = doubleRank.intValue();
                    } else {
                        rank = null;
                    }
                } catch (Exception ex) {
                    log.error("第{}行：无法解析Rank值", rowNum);
                    rank = null;
                }
            }
            
            if (rank == null || rank < 1 || rank > 8) {
                errorMessages.add(String.format("第%d行：Rank值无效（应在1到8之间）", rowNum));
                return null;
            }
            data.setRank(rank);

            // 获取并验证MCS，范围：0-28，使用更宽松的验证方式
            Integer mcsInt;
            Double mcs;
            try {
                Cell mcsCell = row.getCell(5);
                if (mcsCell == null) {
                    mcsInt = null;
                    mcs = null;
                } else if (mcsCell.getCellType() == CellType.NUMERIC) {
                    // 直接获取数值并转为整数
                    double numValue = mcsCell.getNumericCellValue();
                    mcsInt = (int) Math.round(numValue);
                    mcs = numValue;
                } else if (mcsCell.getCellType() == CellType.STRING) {
                    // 尝试将字符串解析为数字
                    try {
                        double numValue = Double.parseDouble(mcsCell.getStringCellValue().trim());
                        mcsInt = (int) Math.round(numValue);
                        mcs = numValue;
                    } catch (NumberFormatException e) {
                        mcsInt = null;
                        mcs = null;
                    }
                } else {
                    mcsInt = null;
                    mcs = null;
                }
            } catch (Exception e) {
                log.error("第{}行：MCS解析错误 - {}", rowNum, e.getMessage());
                mcsInt = null;
                mcs = null;
            }
            
            if (mcsInt == null || mcsInt < 0 || mcsInt > 28) {
                errorMessages.add(String.format("第%d行：MCS值无效（应在0到28之间）", rowNum));
                return null;
            }
            data.setMcs(mcs);

            // 获取并验证PRB数，5G最大可达275，使用更宽松的验证方式
            Integer prbNum;
            try {
                Cell prbCell = row.getCell(6);
                if (prbCell == null) {
                    prbNum = null;
                } else if (prbCell.getCellType() == CellType.NUMERIC) {
                    // 直接获取数值并转为整数
                    double numValue = prbCell.getNumericCellValue();
                    prbNum = (int) Math.round(numValue);
                } else if (prbCell.getCellType() == CellType.STRING) {
                    // 尝试将字符串解析为数字
                    try {
                        double numValue = Double.parseDouble(prbCell.getStringCellValue().trim());
                        prbNum = (int) Math.round(numValue);
                    } catch (NumberFormatException e) {
                        prbNum = null;
                    }
                } else {
                    prbNum = null;
                }
            } catch (Exception e) {
                log.error("第{}行：PRB数解析错误 - {}", rowNum, e.getMessage());
                prbNum = null;
            }
            
            if (prbNum == null || prbNum < 0) {
                errorMessages.add(String.format("第%d行：PRB数无效（应大于0）", rowNum));
                return null;
            }
            // 验证但允许大于正常范围的值
            if (prbNum > 275) {
                log.warn("第{}行：PRB数{}超出正常最大值275", rowNum, prbNum);
            }
            data.setPrbNum(prbNum);

            // 获取并验证BLER值 - 根据提供的Excel截图，BLER值在K列，通常为索引10
            // 但有时Excel的列索引可能与预期不同，因此添加更多日志和容错逻辑
            try {
                // 首先尝试从K列(索引10)读取
                Double bler = getCellValueAsDouble(row.getCell(10));
                
                // 如果值无效，尝试从其他可能的列读取
                if (bler == null || bler < 0) {
                    // 尝试从图中第二张图片看起来像是第8列读取
                    bler = getCellValueAsDouble(row.getCell(8));
                    log.info("第{}行：从备选列(索引8)读取BLER值: {}", rowNum, bler);
                }
                
                if (bler == null || bler < 0) {
                    // 再尝试一些其他可能的列
                    for (int col = 7; col <= 12; col++) {
                        if (col == 8 || col == 10) continue; // 已经尝试过的列
                        Double tryBler = getCellValueAsDouble(row.getCell(col));
                        if (tryBler != null && tryBler >= 0) {
                            bler = tryBler;
                            log.info("第{}行：从列索引{}读取到有效BLER值: {}", rowNum, col, bler);
                            break;
                        }
                    }
                }
                
                // 如果仍然找不到，记录错误但使用0作为默认值
                if (bler == null || bler < 0) {
                    log.warn("第{}行：无法找到有效的BLER值，使用默认值0", rowNum);
                    bler = 0.0;
                }
                
                data.setBler(bler);
                log.debug("第{}行：最终使用的BLER值: {}", rowNum, bler);
                
            } catch (Exception e) {
                log.error("读取BLER值发生错误", e);
                data.setBler(0.0); // 使用默认值
            }

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
                            // 直接获取LocalDateTime以保留毫秒精度
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
                    String dateStr = cell.getStringCellValue().trim();
                    if (dateStr.isEmpty()) {
                        return null;
                    }

                    // 尝试使用所有支持的日期格式
                    for (DateTimeFormatter formatter : DATE_FORMATTERS) {
                        try {
                            // 对于没有年份的时间格式，需要添加当前年月日
                            if (dateStr.matches("\\d{2}:\\d{2}:\\d{2}(\\.\\d{1,3})?")) {
                                LocalDateTime now = LocalDateTime.now();
                                dateStr = String.format("%04d-%02d-%02d %s",
                                    now.getYear(), now.getMonthValue(), now.getDayOfMonth(), dateStr);
                                return LocalDateTime.parse(dateStr, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS"));
                            }

                            return LocalDateTime.parse(dateStr, formatter);
                        } catch (DateTimeParseException e) {
                            // 继续尝试下一个格式
                        }
                    }

                    // 所有标准格式都无法解析，尝试自定义解析
                    log.warn("无法使用标准格式解析日期时间: {}", dateStr);
                    try {
                        // 尝试从字符串中提取时间部分并解析
                        if (dateStr.contains(":")) {
                            String[] parts = dateStr.split("[ T]");
                            for (String part : parts) {
                                if (part.contains(":")) {
                                    // 找到可能是时间的部分
                                    LocalDateTime now = LocalDateTime.now();
                                    String timeStr = String.format("%04d-%02d-%02d %s",
                                        now.getYear(), now.getMonthValue(), now.getDayOfMonth(), part);
                                    return LocalDateTime.parse(timeStr, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
                                }
                            }
                        }
                    } catch (Exception e) {
                        log.debug("自定义解析日期时间失败", e);
                    }

                    // 无法解析，记录错误并返回null
                    log.error("无法解析日期时间: {}", dateStr);
                    break;
            }
        } catch (Exception e) {
            log.error("获取日期时间值时发生错误", e);
        }

        return null;
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
                    try {
                        return Double.parseDouble(value);
                    } catch (NumberFormatException e) {
                        log.error("无法将字符串值转换为数值: {}", value);
                        throw new IllegalArgumentException("单元格包含非数值文本: " + value);
                    }
                case BLANK:
                    return null;
                case FORMULA:
                    try {
                        return cell.getNumericCellValue();
                    } catch (Exception e) {
                        log.error("无法获取公式单元格的数值结果: {}", e.getMessage());
                        throw new IllegalArgumentException("公式单元格计算错误");
                    }
                case BOOLEAN:
                    throw new IllegalArgumentException("单元格包含布尔值，需要数值");
                case ERROR:
                    throw new IllegalArgumentException("单元格包含错误值");
                default:
                    throw new IllegalArgumentException("单元格类型不支持转换为数值");
            }
        } catch (Exception e) {
            log.error("解析数值时发生错误: {}", e.getMessage());
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
                    double numValue = cell.getNumericCellValue();
                    // 检查数值是否为整数
                    if (Math.floor(numValue) != numValue) {
                        log.error("单元格包含小数而非整数: {}", numValue);
                        throw new IllegalArgumentException("需要整数值，但单元格包含小数: " + numValue);
                    }
                    return (int) numValue;
                case STRING:
                    String value = cell.getStringCellValue().trim();
                    if (value.isEmpty()) {
                        return null;
                    }
                    try {
                        return Integer.parseInt(value);
                    } catch (NumberFormatException e) {
                        log.error("无法将字符串值转换为整数: {}", value);
                        throw new IllegalArgumentException("单元格包含非整数文本: " + value);
                    }
                case BLANK:
                    return null;
                case FORMULA:
                    try {
                        double formulaValue = cell.getNumericCellValue();
                        if (Math.floor(formulaValue) != formulaValue) {
                            throw new IllegalArgumentException("公式结果不是整数: " + formulaValue);
                        }
                        return (int) formulaValue;
                    } catch (Exception e) {
                        log.error("无法获取公式单元格的整数结果: {}", e.getMessage());
                        throw new IllegalArgumentException("公式单元格计算错误");
                    }
                case BOOLEAN:
                    throw new IllegalArgumentException("单元格包含布尔值，需要整数");
                case ERROR:
                    throw new IllegalArgumentException("单元格包含错误值");
                default:
                    throw new IllegalArgumentException("单元格类型不支持转换为整数");
            }
        } catch (Exception e) {
            log.error("解析整数时发生错误: {}", e.getMessage());
            throw new IllegalArgumentException("整数格式错误: " + e.getMessage());
        }
    }

    /**
     * 将java.util.Date转换为LocalDateTime
     */
    private LocalDateTime convertDateToLocalDateTime(java.util.Date date) {
        if (date == null) {
            return null;
        }
        return date.toInstant()
            .atZone(java.time.ZoneId.systemDefault())
            .toLocalDateTime();
    }

    /**
     * 尝试将字符串解析为日期时间
     */
    private LocalDateTime parseStringAsDateTime(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }

        value = value.trim();

        // 尝试使用不同的格式解析
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
                // 继续尝试其他格式
            }
        }

        return null;
    }

    /**
     * 验证Excel文件结构，检查必要的列是否存在以及数据类型是否正确
     * @param sheet Excel表格
     * @return ApiResponse 验证结果
     */
    private ApiResponse<?> validateExcelStructure(Sheet sheet) {
        if (sheet == null || sheet.getPhysicalNumberOfRows() < 2) {
            return ApiResponse.error(400, "Excel文件为空或不包含足够的数据行");
        }

        // 检查必要的列是否存在
        List<String> missingColumns = new ArrayList<>();
        
        // 尝试检查多行而不只是第一行，提高容错性
        int lastRowToCheck = Math.min(sheet.getLastRowNum(), 10); // 检查前10行或全部行
        boolean hasValidRow = false;
        
        // 首先只检查列是否存在，不检查数据类型
        for (int rowIndex = 1; rowIndex <= lastRowToCheck; rowIndex++) {
            Row row = sheet.getRow(rowIndex);
            if (row == null) continue;
            
            // 检查每一列是否存在
            boolean rowHasAllColumns = true;
            for (int colIndex = 0; colIndex <= 6; colIndex++) {
                if (row.getCell(colIndex) == null) {
                    rowHasAllColumns = false;
                    break;
                }
            }
            
            if (rowHasAllColumns) {
                hasValidRow = true;
                break;
            }
        }
        
        if (!hasValidRow) {
            // 如果没有找到包含所有必要列的行，添加缺失列信息
            missingColumns.add("文件必须包含至少以下7列：测试时间、RSRP、SINR、MAC吞吐量、Rank、MCS、PRB数");
        }
        
        // 如果列缺失，直接返回错误
        if (!missingColumns.isEmpty()) {
            StringBuilder errorMessage = new StringBuilder("Excel文件格式错误：\n");
            errorMessage.append("缺少必要列: ").append(String.join(", ", missingColumns));
            return ApiResponse.error(400, errorMessage.toString());
        }
        
        // 如果有所有必要的列，尝试在多行中验证数据类型
        List<String> invalidDataTypes = new ArrayList<>();
        
        // 设置用于数据类型检查的行数量（不超过总行数）
        int rowsToValidate = Math.min(sheet.getLastRowNum(), 5);
        boolean[] columnValidations = new boolean[7]; // 记录每列是否至少有一行数据有效
        
        // 检查前几行的数据类型
        for (int rowIndex = 1; rowIndex <= rowsToValidate; rowIndex++) {
            Row row = sheet.getRow(rowIndex);
            if (row == null) continue;
            
            // 检查时间列（索引0）
            if (!columnValidations[0] && row.getCell(0) != null) {
                try {
                    LocalDateTime timeValue = getCellValueAsDateTime(row.getCell(0));
                    if (timeValue != null) {
                        columnValidations[0] = true;
                    }
                } catch (Exception ignored) {
                    // 忽略单行错误，继续检查其他行
                }
            }
            
            // 检查RSRP列（索引1）
            if (!columnValidations[1] && row.getCell(1) != null) {
                try {
                    Double rsrpValue = getCellValueAsDouble(row.getCell(1));
                    if (rsrpValue != null) {
                        columnValidations[1] = true;
                    }
                } catch (Exception ignored) {
                    // 忽略单行错误
                }
            }
            
            // 检查SINR列（索引2）
            if (!columnValidations[2] && row.getCell(2) != null) {
                try {
                    Double sinrValue = getCellValueAsDouble(row.getCell(2));
                    if (sinrValue != null) {
                        columnValidations[2] = true;
                    }
                } catch (Exception ignored) {
                    // 忽略单行错误
                }
            }
            
            // 检查MAC吞吐量列（索引3）
            if (!columnValidations[3] && row.getCell(3) != null) {
                try {
                    Double macValue = getCellValueAsDouble(row.getCell(3));
                    if (macValue != null) {
                        columnValidations[3] = true;
                    }
                } catch (Exception ignored) {
                    // 忽略单行错误
                }
            }
            
            // 检查Rank列（索引4）
            if (!columnValidations[4] && row.getCell(4) != null) {
                try {
                    // 对于Rank，尝试先转换为double，容错性更好
                    try {
                        Double rankValue = getCellValueAsDouble(row.getCell(4));
                        if (rankValue != null && rankValue >= 1 && rankValue <= 8) {
                            columnValidations[4] = true;
                        }
                    } catch (Exception e) {
                        // 如果不是有效的double，尝试作为integer
                        Integer rankValue = getCellValueAsInteger(row.getCell(4));
                        if (rankValue != null && rankValue >= 1 && rankValue <= 8) {
                            columnValidations[4] = true;
                        }
                    }
                } catch (Exception ignored) {
                    // 忽略单行错误
                }
            }
            
            // 检查MCS列（索引5）- 特别关注，使用更宽松的验证
            if (!columnValidations[5] && row.getCell(5) != null) {
                try {
                    Cell mcsCell = row.getCell(5);
                    if (mcsCell.getCellType() == CellType.NUMERIC) {
                        // 直接获取数值，允许小数但会在处理时转为整数
                        double numValue = mcsCell.getNumericCellValue();
                        if (numValue >= 0 && numValue <= 28) {
                            columnValidations[5] = true;
                        }
                    } else if (mcsCell.getCellType() == CellType.STRING) {
                        // 尝试将字符串解析为数字
                        try {
                            double numValue = Double.parseDouble(mcsCell.getStringCellValue().trim());
                            if (numValue >= 0 && numValue <= 28) {
                                columnValidations[5] = true;
                            }
                        } catch (NumberFormatException ignored) {
                            // 非数字字符串，继续检查其他行
                        }
                    }
                } catch (Exception ignored) {
                    // 忽略单行错误
                }
            }
            
            // 检查PRB数列（索引6）
            if (!columnValidations[6] && row.getCell(6) != null) {
                try {
                    // 对于PRB，也先尝试作为double，然后是integer
                    try {
                        Double prbValue = getCellValueAsDouble(row.getCell(6));
                        if (prbValue != null && prbValue >= 0) {
                            columnValidations[6] = true;
                        }
                    } catch (Exception e) {
                        Integer prbValue = getCellValueAsInteger(row.getCell(6));
                        if (prbValue != null && prbValue >= 0) {
                            columnValidations[6] = true;
                        }
                    }
                } catch (Exception ignored) {
                    // 忽略单行错误
                }
            }
            
            // 如果所有列都通过验证，提前结束
            if (allColumnsValid(columnValidations)) {
                break;
            }
        }
        
        // 检查哪些列没有通过验证
        if (!allColumnsValid(columnValidations)) {
            if (!columnValidations[0]) invalidDataTypes.add("测试时间(第1列)应为有效的日期时间格式");
            if (!columnValidations[1]) invalidDataTypes.add("RSRP(第2列)应为数值类型");
            if (!columnValidations[2]) invalidDataTypes.add("SINR(第3列)应为数值类型");
            if (!columnValidations[3]) invalidDataTypes.add("MAC吞吐量(第4列)应为数值类型");
            if (!columnValidations[4]) invalidDataTypes.add("Rank(第5列)应为整数类型(1-8)");
            if (!columnValidations[5]) invalidDataTypes.add("MCS(第6列)应为整数类型(0-28)");
            if (!columnValidations[6]) invalidDataTypes.add("PRB数(第7列)应为整数类型");
        }
        
        // 如果存在无效数据类型，返回错误响应
        if (!invalidDataTypes.isEmpty()) {
            StringBuilder errorMessage = new StringBuilder("Excel文件格式错误：\n");
            errorMessage.append("数据类型错误: ").append(String.join(", ", invalidDataTypes));
            return ApiResponse.error(400, errorMessage.toString());
        }
        
        return ApiResponse.success(null);
    }
    
    /**
     * 检查所有列是否都通过验证
     */
    private boolean allColumnsValid(boolean[] columnValidations) {
        for (boolean valid : columnValidations) {
            if (!valid) return false;
        }
        return true;
    }
}