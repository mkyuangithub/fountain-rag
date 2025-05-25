package com.mkyuan.fountainbase.office.support;

import com.mkyuan.fountainbase.common.util.okhttp.OkHttpHelper;
import com.mkyuan.fountainbase.office.FileBean;
import jodd.util.StringUtil;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.pdfbox.contentstream.PDFGraphicsStreamEngine;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDResources;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDSimpleFont;
import org.apache.pdfbox.pdmodel.graphics.image.PDImage;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.pdfbox.util.Matrix;
import org.apache.pdfbox.util.Vector;

import javax.imageio.ImageIO;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;

public class FileResovlerForPdf extends AbstractFileResolver {
    protected Logger logger = LogManager.getLogger(this.getClass());

    public FileResovlerForPdf(int maxReadLength, boolean readImg, String apiKey, OkHttpHelper okHttpHelper,
                              String ocrUrl) {

        super(maxReadLength, readImg, apiKey, okHttpHelper, ocrUrl);
    }

    @Override
    protected FileBean doResolve(String fileName, byte[] fileData) throws IOException {
        StringBuilder fileContent = new StringBuilder();

        try (PDDocument document = PDDocument.load(new ByteArrayInputStream(fileData))) {
            List<String> imgs = new ArrayList<>();

            int pageSize = document.getNumberOfPages();
            // 一页一页读取
            for (int i = 0; i < pageSize; i++) {
                // 文本内容
                PDFTextStripper stripper = new PDFTextStripper();
                // 设置按顺序输出
                stripper.setSortByPosition(true);
                stripper.setStartPage(i + 1);
                stripper.setEndPage(i + 1);
                String textContent = stripper.getText(document);
                textContent = textContent.replaceAll("(?m)^[ \t]*\r?\n", "")
                                         .replaceAll("\n{2,}", "\n")
                                         .trim();
                fileContent.append(textContent);
                // 图片内容
                PDPage page = document.getPage(i);
                PDResources resources = page.getResources();
                Iterable<COSName> cosNames = resources.getXObjectNames();
                if (cosNames != null) {
                    Iterator<COSName> cosNamesIter = cosNames.iterator();
                    while (cosNamesIter.hasNext()) {
                        COSName cosName = cosNamesIter.next();
                        if (resources.isImageXObject(cosName) && readImg) {
                            PDImageXObject picture = (PDImageXObject) resources.getXObject(cosName);
                            // 获取图片数据
                            BufferedImage image = picture.getImage();

                            // 将BufferedImage转换为byte[]
                            ByteArrayOutputStream baos = new ByteArrayOutputStream();
                            try {
                                // 可以选择图片格式，这里使用PNG
                                ImageIO.write(image, "png", baos);
                                byte[] imageBytes = baos.toByteArray();
                                String ocrResult = super.sendImageToOcr(okHttpHelper, apiKey, imageBytes);
                                // 添加OCR结果到内容
                                if (ocrResult != null && !ocrResult.isEmpty()) {
                                    fileContent.append("\n");
                                    fileContent.append(ocrResult);
                                    fileContent.append("\n");
                                }
                            } catch (IOException e) {
                                // 处理异常
                                logger.error(">>>>>>读取pdf里的图片出错->{}", e.getMessage(), e);
                            } finally {
                                try {
                                    baos.close();
                                } catch (IOException e) {
                                }
                            }
                        }
                    }
                }
                fileContent.append('\f');
            }

            logger.info(">>>>>>fileContent->{}", fileContent.toString());
            return FileBean.ofPdf(fileContent.toString());
        }
    }

}
