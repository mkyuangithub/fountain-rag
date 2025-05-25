package com.mkyuan.fountainbase.common.util;

import org.apache.commons.lang3.StringUtils;

public class FileType {
    private static final String EXCEL_TYPE_OLD = ".xls";
    private static final String EXCEL_TYPE_NEW = ".xlsx";
    public static boolean isExcel(String fileName) {
        return StringUtils.endsWith(fileName, EXCEL_TYPE_OLD) || StringUtils.endsWith(fileName, EXCEL_TYPE_NEW);
    }
}
