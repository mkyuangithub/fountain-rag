package com.mkyuan.fountainbase.office.support;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.mkyuan.fountainbase.office.FileBean;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.json.JSONObject;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.*;

public class FileResolverForXlsx extends AbstractFileResolverForExcel {

    protected Logger logger = LogManager.getLogger(this.getClass());
    public FileResolverForXlsx(int maxRow, int maxCol, int maxReadLength,boolean readImg) {

        super(maxRow, maxCol, maxReadLength,false,null,null,null);
    }

    @Override
    public FileBean doResolve(String fileName, byte[] fileData) throws IOException {
        DataFormatter textFormatter = new DataFormatter();
        SimpleDateFormat dateTimeFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");

        List<String> headers = new ArrayList<>();
        //List<Map<Integer, String>> dataList = new ArrayList<>();
        List<Map<String,String>> dataList=new ArrayList<>();
        int maxRowNums = this.maxRow;
        int maxColNums = this.maxCol;
        int maxReadStrLen = this.maxReadLength;
        try (ByteArrayInputStream bais = new ByteArrayInputStream(fileData); Workbook workbook = new XSSFWorkbook(bais)) {
            Sheet sheet = workbook.getSheetAt(0);
            FormulaEvaluator evaluator = workbook.getCreationHelper().createFormulaEvaluator();

            // 获取所有行
            int firstRowNum = sheet.getFirstRowNum();
            int lastRowNum = sheet.getLastRowNum();

            if (lastRowNum < firstRowNum) {
                logger.error(">>>>>>要被解释的->{} 文件是一个空表格", fileName);
                return null;
            }

            // 读取第一行作为表头
            Row headerRow = sheet.getRow(firstRowNum);
            if (headerRow == null) {
                logger.error(">>>>>>要被解释的->{} 文件表头行为空", fileName);
                return null;
            }

            List<String> headerValues = new ArrayList<>();
            for (Cell cell : headerRow) {
                String cellValue = getCellValueAsString(cell, evaluator, maxReadStrLen);
                headers.add(cellValue);
                headerValues.add(cellValue);
            }
            logger.info(">>>>>>读取表头行: {}, 表头大小: {}", firstRowNum, headers.size());

            // 从第二行开始读取数据行
            int rowCount = 0;
            for (int rowIndex = firstRowNum + 1; rowIndex <= lastRowNum && rowCount < maxRowNums; rowIndex++) {
                Row row = sheet.getRow(rowIndex);
                if (row == null) continue;

                //Map<Integer, String> data = new LinkedHashMap<>();
                Map<String,String> data=new HashMap();
                String img="";
                StringBuilder content=new StringBuilder();
                int cellNum = Math.min(row.getLastCellNum(), headers.size());

                // 检查此行是否为空行
                boolean isEmptyRow = true;
                for (int i = 0; i < cellNum; i++) {
                    Cell cell = row.getCell(i, Row.MissingCellPolicy.CREATE_NULL_AS_BLANK);
                    String cellValue = getCellValueAsString(cell, evaluator, maxReadStrLen);
                    if (cellValue != null && !cellValue.trim().isEmpty()) {
                        isEmptyRow = false;
                        break;
                    }
                }

                // 如果是空行，跳过此行处理
                if (isEmptyRow) {
                    logger.info(">>>>>>跳过空行: {}", rowIndex);
                    continue;
                }

                // 处理非空行的数据
                for (int i = 0; i < cellNum; i++) {
                    try {
                        Cell cell = row.getCell(i, Row.MissingCellPolicy.CREATE_NULL_AS_BLANK);
                        String cellValue = getCellValueAsString(cell, evaluator, maxReadStrLen);

                        // 防空指针处理
                        String headerValue = (i < headerValues.size() && headerValues.get(i) != null) ? headerValues.get(i) : "";
                        cellValue = (cellValue != null) ? cellValue : "";
                        if(!FileBean.isValidUrl(cellValue)){
                            content.append( headerValue + "：" + cellValue + " ");
                        }else{
                            img=cellValue;
                        }
                        // 格式化为 "表头：值" 的格式
                        //data.put(i, headerValue + "：" + cellValue + " ");

                        // 列数限制检查
                        if (maxColNums > 0 && i >= maxColNums) {
                            break;
                        }
                    } catch(Exception e) {
                        logger.error(">>>>>>解析excel遇有错误行，跳过->{}",e.getMessage(),e);
                    }
                }
                data.put("content",content.toString());
                data.put("img",img);
                logger.info(">>>>>>excel content->{}",data.toString());
                dataList.add(data);
                rowCount++;
            }
        }
        return FileBean.ofExcel(headers, dataList);
    }

    // 辅助方法：获取单元格的字符串值
    private String getCellValueAsString(Cell cell, FormulaEvaluator evaluator, int maxReadStrLen) {
        if (cell == null) {
            return "";
        }

        String cellValue = "";
        try {
            switch (cell.getCellType()) {
                case STRING:
                    cellValue = cell.getStringCellValue();
                    break;
                case NUMERIC:
                    if (DateUtil.isCellDateFormatted(cell)) {
                        Date date = cell.getDateCellValue();
                        if (DateUtil.isValidExcelDate(date.getTime())) {
                            Calendar calendar = Calendar.getInstance();
                            calendar.setTime(date);
                            if (calendar.get(Calendar.HOUR_OF_DAY) == 0 &&
                                    calendar.get(Calendar.MINUTE) == 0 &&
                                    calendar.get(Calendar.SECOND) == 0 &&
                                    calendar.get(Calendar.MILLISECOND) == 0) {
                                cellValue = new SimpleDateFormat("yyyy-MM-dd").format(date) + " 00:00:00";
                            } else {
                                cellValue = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(date);
                            }
                        }
                    } else {
                        double numericValue = cell.getNumericCellValue();
                        if (numericValue == Math.floor(numericValue)) {
                            cellValue = String.valueOf((int) numericValue);
                        } else {
                            cellValue = String.valueOf(numericValue);
                        }
                    }
                    break;
                case FORMULA:
                    CellValue cellValue1 = evaluator.evaluate(cell);
                    switch (cellValue1.getCellType()) {
                        case NUMERIC:
                            double formulaNumericValue = cellValue1.getNumberValue();
                            if (formulaNumericValue == Math.floor(formulaNumericValue)) {
                                cellValue = String.valueOf((int) formulaNumericValue);
                            } else {
                                cellValue = String.valueOf(formulaNumericValue);
                            }
                            break;
                        case STRING:
                            cellValue = cellValue1.getStringValue();
                            break;
                        default:
                            break;
                    }
                    break;
                default:
                    cellValue = cell.toString();
            }

            if (cellValue == null) {
                cellValue = "";
            }

            if (maxReadStrLen > 0 && cellValue.length() > maxReadStrLen) {
                cellValue = cellValue.substring(0, maxReadStrLen);
            }
        } catch (Exception e) {
            logger.error(">>>>>>获取单元格值发生错误：{}", e.getMessage(), e);
            cellValue = "";
        }

        return cellValue;
    }

}
