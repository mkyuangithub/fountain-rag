package com.mkyuan.fountainbase.office.support;

import com.mkyuan.fountainbase.common.util.okhttp.OkHttpHelper;
import com.mkyuan.fountainbase.office.FileBean;
import jodd.util.StringUtil;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.poi.xwpf.usermodel.*;
import org.apache.poi.ooxml.POIXMLDocumentPart;
import org.apache.poi.xssf.usermodel.XSSFDrawing;
import org.apache.xmlbeans.XmlObject;
import org.openxmlformats.schemas.drawingml.x2006.main.CTGraphicalObject;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTDrawing;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class FileResolverForDocx extends AbstractFileResolverForWord {
    protected Logger logger = LogManager.getLogger(this.getClass());
    public FileResolverForDocx(int maxReadLength, boolean readImg, String apiKey, OkHttpHelper okHttpHelper,String ocrUrl) {
        super(maxReadLength,readImg,apiKey,okHttpHelper,ocrUrl);
    }

    @Override
    protected FileBean doResolve(String fileName, byte[] fileData) throws IOException {
        int maxReadStrLen = this.maxReadLength;
        StringBuilder contentBuilder = new StringBuilder();
        try (ByteArrayInputStream bais = new ByteArrayInputStream(fileData); XWPFDocument document = new XWPFDocument(bais)) {
            // 按顺序处理文档体内容
            processBodyElements(document, contentBuilder);

            String text = contentBuilder.toString();

            if (maxReadStrLen > 0) {
                return FileBean.ofWord(StringUtils.substring(text, 0, maxReadStrLen));
            }
            return FileBean.ofWord(text);
        }
    }

    /**
     * 按顺序处理文档体中的元素
     */
    private void processBodyElements(XWPFDocument document, StringBuilder contentBuilder) {
        // 获取文档中所有图片，用于后续判断
        List<XWPFPictureData> allPictures = document.getAllPictures();
        boolean hasPictures = !allPictures.isEmpty();

        // 如果有图片，进行图片判断（具体处理留空）
        if (hasPictures) {
            logger.info(">>>>>>文档中包含{}张图片", allPictures.size());
            /*
             * 图片处理逻辑，暂不处理
             * 这里可以放置图片处理代码
             * 例如：
             * for (XWPFPictureData pictureData : allPictures) {
             *     // 图片处理逻辑
             * }
             */
        }

        // 遍历文档中的所有元素，按照它们在文档中出现的顺序
        for (IBodyElement element : document.getBodyElements()) {
            if (element instanceof XWPFParagraph) {
                processParagraph((XWPFParagraph) element, contentBuilder);
            } else if (element instanceof XWPFTable) {
                processTable((XWPFTable) element, contentBuilder);
            }
        }

        // 处理SmartArt内容
        processSmartArt(document, contentBuilder);
    }

    /**
     * 处理段落
     */
    private void processParagraph(XWPFParagraph paragraph, StringBuilder contentBuilder) {
        // 检查段落中是否包含图片
        boolean hasPictures = false;
        List<XWPFRun> runs = paragraph.getRuns();
        for (XWPFRun run : runs) {
            List<XWPFPicture> pictures = run.getEmbeddedPictures();
            if (!pictures.isEmpty()) {
                hasPictures = true;
                break;
            }
        }

        // 如果段落包含图片，进行图片判断（具体处理留空）
        if (hasPictures && readImg) {
            logger.info(">>>>>>段落中包含图片");
            // 处理段落中的所有图片
            for (XWPFRun run : runs) {
                List<XWPFPicture> pictures = run.getEmbeddedPictures();
                for (XWPFPicture picture : pictures) {
                    try {
                        // 获取图片数据
                        byte[] imageBytes = picture.getPictureData().getData();

                        // 调用OCR服务
                        String ocrResult = super.sendImageToOcr(okHttpHelper,apiKey,imageBytes);
                        // 添加OCR结果到内容
                        if (ocrResult != null && !ocrResult.isEmpty()) {
                            contentBuilder.append("\n");
                            contentBuilder.append(ocrResult);
                            contentBuilder.append("\n");
                        }
                    } catch (Exception e) {
                        logger.error(">>>>>>处理段落中图片失败->{}", e.getMessage(),e);
                    }
                }
            }
        }

        // 处理段落文本
        String paragraphText = paragraph.getText().trim();
        if (!paragraphText.isEmpty()) {
            // 检查是否是章节标题
            if (paragraph.getStyle() != null && paragraph.getStyle().toLowerCase().contains("heading")) {
                // 章节标题后添加两个换行
                contentBuilder.append(paragraphText).append("\n\n");
            } else {
                // 普通段落后添加一个换行
                contentBuilder.append(paragraphText).append("\n");
            }
        }
    }

    /**
     * 处理表格
     */
    private void processTable(XWPFTable table, StringBuilder contentBuilder) {
        contentBuilder.append("\n"); // 表格前添加换行
        // 遍历表格的每一行
        for (XWPFTableRow row : table.getRows()) {
            // 遍历行中的每个单元格
            for (XWPFTableCell cell : row.getTableCells()) {
                // 检查单元格中是否包含图片
                boolean hasPictures = false;
                for (XWPFParagraph paragraph : cell.getParagraphs()) {
                    for (XWPFRun run : paragraph.getRuns()) {
                        if (!run.getEmbeddedPictures().isEmpty()) {
                            hasPictures = true;
                            break;
                        }
                    }
                    if (hasPictures) break;
                }

                // 如果单元格包含图片，进行图片判断（具体处理留空）
                if (hasPictures) {
                    logger.info(">>>>>>表格单元格中包含图片");
                    /*
                     * 表格单元格中图片处理逻辑，暂不处理
                     * 这里可以放置图片处理代码
                     */
                }

                // 获取单元格中的文本内容
                String cellText = cell.getText().trim();
                if (!cellText.isEmpty()) {
                    contentBuilder.append(cellText).append(" "); // 使用空格分隔单元格
                }
            }
            contentBuilder.append("\n"); // 每行结束添加换行
        }
        contentBuilder.append("\n"); // 表格结束后添加额外的换行
    }

    /**
     * 处理SmartArt
     */
    private void processSmartArt(XWPFDocument document, StringBuilder contentBuilder) {
        // 使用Set来存储已处理的SmartArt文本，避免重复
        Set<String> processedTexts = new LinkedHashSet<>();

        // 遍历所有关系
        for (POIXMLDocumentPart part : document.getRelations()) {
            String partName = part.getPackagePart().getPartName().getName().toLowerCase();
            // 只处理data文件，忽略其他类型
            if (partName.matches(".*/diagrams/data\\d+\\.xml$")) {
                logger.info(">>>>>>处理diagram data文件: {}", partName);
                try {
                    String diagramXml = new String(part.getPackagePart().getInputStream().readAllBytes(), StandardCharsets.UTF_8);
                    List<String> smartArtTexts = extractSmartArtTextFromDiagram(diagramXml);
                    // 添加到Set中自动去重
                    processedTexts.addAll(smartArtTexts);
                } catch (Exception e) {
                    logger.error("Error reading diagram part", e);
                }
            }
        }

        // 将处理后的文本添加到contentBuilder
        if (!processedTexts.isEmpty()) {
            contentBuilder.append("\n");
            for (String text : processedTexts) {
                contentBuilder.append(text);
            }
        }
    }

    private List<String> extractSmartArtTextFromDiagram(String diagramXml) {
        List<String> texts = new ArrayList<>();
        try {
            // 使用正则表达式匹配<a:t>标签中的文本
            Pattern pattern = Pattern.compile("<a:t>([^<]+?)</a:t>");
            Matcher matcher = pattern.matcher(diagramXml);

            while (matcher.find()) {
                String content = matcher.group(1).trim();
                if (!content.isEmpty()) {
                    logger.info(">>>>>>找到SmartArt文本内容: {}", content);
                    texts.add(content+"\n");
                }
            }
        } catch (Exception e) {
            logger.error("Error extracting SmartArt text", e);
        }
        return texts;
    }
}
