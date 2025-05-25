package com.mkyuan.fountainbase.office.support;

import com.mkyuan.fountainbase.office.FileBean;
import org.apache.commons.lang3.StringUtils;
import org.apache.poi.hwpf.HWPFDocument;
import org.apache.poi.hwpf.model.PicturesTable;
import org.apache.poi.hwpf.usermodel.Picture;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
public class FileResolverForTxt extends AbstractFileResolver{
    public FileResolverForTxt(int maxReadLength) {
        super(maxReadLength,false,null,null,null);
    }
    @Override
    protected FileBean doResolve(String fileName, byte[] fileData) throws IOException {
        int maxReadStrLen = this.maxReadLength;
        // 使用InputStreamReader来正确处理字符编码
        try (ByteArrayInputStream bais = new ByteArrayInputStream(fileData);
             InputStreamReader isr = new InputStreamReader(bais, StandardCharsets.UTF_8);
             BufferedReader reader = new BufferedReader(isr)) {

            StringBuilder textBuilder = new StringBuilder();
            String line;

            while ((line = reader.readLine()) != null) {
                // 检查是否已经超过最大长度限制
                if (maxReadStrLen > 0 && textBuilder.length() >= maxReadStrLen) {
                    break;
                }
                textBuilder.append(line);
                textBuilder.append("\n");
            }

            String text = textBuilder.toString();

            // 保留原有的长度限制逻辑
            if (maxReadStrLen > 0) {
                return FileBean.ofWord(StringUtils.substring(text, 0, maxReadStrLen));
            }

            return FileBean.ofWord(text);
        }
    }
}
