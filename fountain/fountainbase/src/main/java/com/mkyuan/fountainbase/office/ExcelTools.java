package com.mkyuan.fountainbase.office;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Cell;
import java.io.ByteArrayOutputStream;
import java.util.List;
import java.util.Map;
@Component
public class ExcelTools {

    protected Logger logger = LogManager.getLogger(this.getClass());
    public byte[] createExcel(List<String> columnNames, List<Map<String, String>> data) throws Exception {
        // 创建一个新的Excel工作簿
        Workbook workbook = new XSSFWorkbook();
        // 创建一个工作表
        Sheet sheet = workbook.createSheet("Data Sheet");

        // 创建标题行
        Row headerRow = sheet.createRow(0);
        for (int i = 0; i < columnNames.size(); i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(columnNames.get(i));
        }

        // 填充数据行
        int rowNum = 1;
        for (Map<String, String> dataRow : data) {
            Row row = sheet.createRow(rowNum++);
            for (int i = 0; i < columnNames.size(); i++) {
                String columnName = columnNames.get(i);
                String value = dataRow.getOrDefault(columnName, "");
                row.createCell(i).setCellValue(value);
            }
        }

        // 将工作簿写入字节数组输出流
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        workbook.write(outputStream);
        workbook.close();

        // 返回字节数组
        return outputStream.toByteArray();
    }
}
