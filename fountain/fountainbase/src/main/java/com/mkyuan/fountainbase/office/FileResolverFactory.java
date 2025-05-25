package com.mkyuan.fountainbase.office;


import com.mkyuan.fountainbase.common.util.okhttp.OkHttpHelper;
import com.mkyuan.fountainbase.office.support.*;
import jodd.util.StringUtil;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Component
public class FileResolverFactory{

    private static final String WORD_TYPE_OLD = ".doc";
    private static final String WORD_TYPE_NEW = ".docx";
    private static final String EXCEL_TYPE_OLD = ".xls";
    private static final String EXCEL_TYPE_NEW = ".xlsx";
    private static final String PDF_TYPE = ".pdf";

    private static final String TXT_TYPE = ".txt";

    private static final String HTML_TYPE_HTML = ".html";
    private static final String HTML_TYPE_HTM = ".htm";

    private static final SimpleDateFormat SDF = new SimpleDateFormat("yyyyMMddHHmm");

    private final Logger logger = LoggerFactory.getLogger(getClass());

    @Value("${fountain.ai.embedding.embeddingApiKey}")
    private String apiKey = "";

    @Value("${fountain.ai.ocr.ocrUrl}")
    private String ocrUrl = "";

    @Autowired
    private OkHttpHelper okHttpHelper;

    public FileResolver getFileResolver(String token, String userName, String fileName, boolean readImg) {
        if (this.isWord(fileName)) {
            if (this.isOfficeNewVersion(fileName)) {
                FileResolverForDocx resolver = new FileResolverForDocx(-1,readImg,apiKey,okHttpHelper,ocrUrl);//不设限全部读
                resolver.setToken(token);
                resolver.setUserName(userName);
                return resolver;
            } else {
                FileResolverForDoc resolver = new FileResolverForDoc(-1,readImg,apiKey,okHttpHelper,ocrUrl);
                resolver.setToken(token);
                resolver.setUserName(userName);
                return resolver;
            }
        } else if (this.isExcel(fileName)) {
            if (this.isOfficeNewVersion(fileName)) {
                FileResolverForXlsx resolver = new FileResolverForXlsx(65535, 10000, -1,false);
                resolver.setToken(token);
                resolver.setUserName(userName);
                return resolver;
            } else {
                FileResolverForXls resolver = new FileResolverForXls(65535, 10000, -1,false);
                resolver.setToken(token);
                resolver.setUserName(userName);
                return resolver;
            }
        } else if (this.isPdf(fileName)) {
            FileResovlerForPdf resolver = new FileResovlerForPdf(-1,readImg,apiKey,okHttpHelper,ocrUrl);
            resolver.setToken(token);
            resolver.setUserName(userName);
            return resolver;
        } else if (this.isTxt(fileName)) {
            FileResolverForTxt resolver = new FileResolverForTxt(-1);
            resolver.setToken(token);
            resolver.setUserName(userName);
            return resolver;
        }
        throw new UnsupportedOperationException("不支持的文件类型");
    }


    public boolean isWord(String fileName) {
        return StringUtils.endsWith(fileName, WORD_TYPE_OLD) || StringUtils.endsWith(fileName, WORD_TYPE_NEW);
    }

    public boolean isTxt(String fileName) {
        return StringUtils.endsWith(fileName, TXT_TYPE);
    }

    public boolean isExcel(String fileName) {
        return StringUtils.endsWith(fileName, EXCEL_TYPE_OLD) || StringUtils.endsWith(fileName, EXCEL_TYPE_NEW);
    }

    public boolean isOfficeNewVersion(String fileName) {
        return StringUtils.endsWith(fileName, WORD_TYPE_NEW) || StringUtils.endsWith(fileName, EXCEL_TYPE_NEW);
    }

    public boolean isPdf(String fileName) {
        return StringUtils.endsWith(fileName, PDF_TYPE);
    }

}
