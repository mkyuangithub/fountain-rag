package com.mkyuan.fountainbase.office.support;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.mkyuan.fountainbase.office.FileBean;
import org.apache.commons.lang3.StringUtils;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.*;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.*;

public class FileResolverForXls extends AbstractFileResolverForExcel {
    private final Logger logger = LoggerFactory.getLogger(getClass());
    public FileResolverForXls(int maxRow, int maxCol, int maxReadLength,boolean readImg) {

        super(maxRow, maxCol, maxReadLength,false,null,null,null);
    }
    @Override
    protected FileBean doResolve(String fileName, byte[] fileData) throws IOException {
        List<String> headers = new ArrayList<>();
        List<Map<String,String>> dataList = new ArrayList<>();
        int maxRowNums = this.maxRow;
        int maxColNums = this.maxCol;
        int maxReadStrLen = this.maxReadLength;
        try (ByteArrayInputStream bais = new ByteArrayInputStream(fileData); Workbook workbook = new HSSFWorkbook(bais)) {
            Sheet sheet = workbook.getSheetAt(0);
            FormulaEvaluator evaluator = workbook.getCreationHelper().createFormulaEvaluator();
            Iterator<Row> rows = sheet.iterator();
            if (!rows.hasNext()) {
                logger.error(">>>>>>要被解释的->{} 文件是一个空表格", fileName);
                return null;
            }
            Row headerRow = rows.next();
            //for (Cell cell : headerRow) {
                //headers.add(cell.getStringCellValue());
            //}
            int rowCount = 0;
            while (rows.hasNext() && rowCount < maxRowNums) {
                Map<String,String> data=new HashMap<>();
                StringBuilder content=new StringBuilder();
                String img="";
                Row row = rows.next();
                //Map<Integer, String> data = new LinkedHashMap<>();
                int cellNum = row.getLastCellNum();
                for (int i = 0; i < cellNum; i++) {
                    Cell cell = row.getCell(i, Row.MissingCellPolicy.CREATE_NULL_AS_BLANK);
                    String cellValue="";
                    switch (cell.getCellType()) {
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
                        case STRING:
                            cellValue = cell.getStringCellValue();
                            break;
                        case NUMERIC:
                            double numericValue = cell.getNumericCellValue();
                            if (numericValue == Math.floor(numericValue)) {
                                cellValue = String.valueOf((int) numericValue);
                            } else {
                                cellValue = String.valueOf(numericValue);
                            }
                            break;
                        default:
                            cellValue = cell.toString();
                    }
                    // 如果单元格的值为空或者是空字符串，那么就跳过当前的迭代
                    if (cellValue == null || cellValue.isEmpty()) {
                        cellValue = "";
                    }
                    // 如果长度大于10个中文字符，截取前10个字符
                    if (maxReadStrLen > 0 && cellValue.length() > maxReadStrLen) {
                        cellValue = cellValue.substring(0, maxReadStrLen);
                    }
                    if(!FileBean.isValidUrl(cellValue)){
                        content.append( cellValue);
                    }else{
                        img=cellValue;
                    }
                    data.put("content",content.toString());
                    data.put("img",img);
                    // 如果已经达到了列数限制，那么就结束循环
                    if (maxColNums > 0 && i >= maxColNums) {
                        break;
                    }
                }
                dataList.add(data);
                rowCount++;
            }
        }
        return FileBean.ofExcel(headers, dataList);
    }

}
