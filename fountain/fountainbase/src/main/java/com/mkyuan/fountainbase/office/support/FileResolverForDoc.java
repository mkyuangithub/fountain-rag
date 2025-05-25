package com.mkyuan.fountainbase.office.support;

import com.mkyuan.fountainbase.common.util.okhttp.OkHttpHelper;
import com.mkyuan.fountainbase.office.FileBean;
//import com.mkyuan.aset.pojo.pictool.PicResultBean;
import jodd.util.StringUtil;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.poi.hwpf.HWPFDocument;
import org.apache.poi.hwpf.model.PicturesTable;
import org.apache.poi.hwpf.usermodel.*;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class FileResolverForDoc extends AbstractFileResolverForWord {
	protected Logger logger = LogManager.getLogger(this.getClass());
	public FileResolverForDoc(int maxReadLength, boolean readImg, String apiKey, OkHttpHelper okHttpHelper,String ocrUrl) {
		super(maxReadLength,readImg,apiKey,okHttpHelper,ocrUrl);
	}
	@Override
	protected FileBean doResolve(String fileName, byte[] fileData) throws IOException {
		int maxReadStrLen = this.maxReadLength;
		StringBuilder contentBuilder = new StringBuilder();
		try (ByteArrayInputStream bais = new ByteArrayInputStream(fileData);
			 HWPFDocument document = new HWPFDocument(bais)) {

			// 处理文档内容
			processDocument(document, contentBuilder);

			String text = contentBuilder.toString();

			if (maxReadStrLen > 0) {
				return FileBean.ofWord(StringUtils.substring(text, 0, maxReadStrLen));
			}
			return FileBean.ofWord(text);
		}
	}

	/**
	 * 处理整个文档
	 */
	private void processDocument(HWPFDocument document, StringBuilder contentBuilder) {
		// 处理文本内容
		processText(document, contentBuilder);

		// 处理图片内容
		if (readImg) {
			processPictures(document, contentBuilder);
		}

		// 处理表格内容
		processTables(document, contentBuilder);
	}

	/**
	 * 处理文档中的文本
	 */
	private void processText(HWPFDocument document, StringBuilder contentBuilder) {
		// 获取文档的文本内容
		String text = document.getDocumentText();
		if (text != null && !text.isEmpty()) {
			contentBuilder.append(text);
		}

		// 也可以使用Range来获取更详细的文本结构
		Range range = document.getRange();
		for (int i = 0; i < range.numParagraphs(); i++) {
			Paragraph paragraph = range.getParagraph(i);
			String paragraphText = paragraph.text().trim();

			// 跳过空段落
			if (paragraphText.isEmpty() || paragraphText.equals("\r")) {
				continue;
			}

			// 检查是否是标题样式
			if (paragraph.isInList() || paragraph.getStyleIndex() > 0) {
				// 可能是标题，添加两个换行
				contentBuilder.append(paragraphText).append("\n\n");
			} else {
				// 普通段落，添加一个换行
				contentBuilder.append(paragraphText).append("\n");
			}
		}
	}

	/**
	 * 处理文档中的图片
	 */
	private void processPictures(HWPFDocument document, StringBuilder contentBuilder) {
		try {
			PicturesTable picturesTable = document.getPicturesTable();
			List<Picture> pictures = picturesTable.getAllPictures();

			if (!pictures.isEmpty()) {
				logger.info(">>>>>>文档中包含{}张图片", pictures.size());

				for (Picture picture : pictures) {
					try {
						// 获取图片数据
						byte[] imageBytes = picture.getContent();

						// 调用OCR服务
						String ocrResult = super.sendImageToOcr(okHttpHelper, apiKey, imageBytes);

						// 添加OCR结果到内容
						if (ocrResult != null && !ocrResult.isEmpty()) {
							contentBuilder.append("\n");
							contentBuilder.append(ocrResult);
							contentBuilder.append("\n");
						}
					} catch (Exception e) {
						logger.error(">>>>>>处理图片失败->{}", e.getMessage(), e);
					}
				}
			}
		} catch (Exception e) {
			logger.error(">>>>>>获取图片失败->{}", e.getMessage(), e);
		}
	}

	/**
	 * 处理文档中的表格
	 */
	private void processTables(HWPFDocument document, StringBuilder contentBuilder) {
		try {
			Range range = document.getRange();
			TableIterator tableIterator = new TableIterator(range);

			while (tableIterator.hasNext()) {
				Table table = tableIterator.next();
				contentBuilder.append("\n"); // 表格前添加换行

				// 遍历表格的每一行
				for (int rowIndex = 0; rowIndex < table.numRows(); rowIndex++) {
					TableRow row = table.getRow(rowIndex);

					// 遍历行中的每个单元格
					for (int colIndex = 0; colIndex < row.numCells(); colIndex++) {
						TableCell cell = row.getCell(colIndex);
						String cellText = cell.text().trim();

						if (!cellText.isEmpty() && !cellText.equals("\r")) {
							contentBuilder.append(cellText).append(" "); // 使用空格分隔单元格
						}
					}
					contentBuilder.append("\n"); // 每行结束添加换行
				}
				contentBuilder.append("\n"); // 表格结束后添加额外的换行
			}
		} catch (Exception e) {
			logger.error(">>>>>>处理表格失败->{}", e.getMessage(), e);
		}
	}
}
