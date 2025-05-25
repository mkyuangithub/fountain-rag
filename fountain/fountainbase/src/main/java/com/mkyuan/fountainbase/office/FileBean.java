package com.mkyuan.fountainbase.office;

import org.apache.commons.lang3.StringUtils;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public class FileBean {

	// 文件类型
	public static final int TYPE_WORD = 1;
	public static final int TYPE_EXCEL = 2;
	public static final int TYPE_PDF = 3;
	public static final int TYPE_TXT = 5;
	private static final char SEP = ',';

	private int type;

	private String pdfContent;


	private String wordContent;
	private List<String> excelHeaders;
	private List<Map<Integer, String>> excelContent;
	private List<Map<String,String>> excelContentJson=new ArrayList<>();

	public List<Map<String, String>> getExcelContentJson() {
		return excelContentJson;
	}

	public void setExcelContentJson(List<Map<String, String>> excelContentJson) {
		this.excelContentJson = excelContentJson;
	}

	public void eachContent(boolean mergeHeader, Consumer<String> consumer) {
		if (this.isWord() && StringUtils.isNotBlank(this.wordContent)) {
			consumer.accept(this.wordContent);
		} else if (this.isExcel()) {
			// 处理表头
			/*
			if (mergeHeader == false && null != this.excelHeaders && this.excelHeaders.size() > 0) {
				consumer.accept(convertListToString(this.excelHeaders) + "\n");
			}
			 */
			// 处理内容，每行添加换行符
			if (null != this.excelContent && this.excelContent.size() > 0) {
				for (Map<Integer, String> map : this.excelContent) {
					consumer.accept(convertMapToString(map) + "\n");
				}
			}
		} else if (this.isPdf()) {
			consumer.accept(this.pdfContent);
		}
	}

	public boolean isWord() {
		return this.type == TYPE_WORD;
	}

	public boolean isExcel() {
		return this.type == TYPE_EXCEL;
	}

	public boolean isPdf() {
		return this.type == TYPE_PDF;
	}

	public int getType() {
		return type;
	}

	public void setType(int type) {
		this.type = type;
	}


	public String getWordContent() {
		return wordContent;
	}

	public void setWordContent(String wordContent) {
		this.wordContent = wordContent;
	}

	public List<Map<Integer, String>> getExcelContent() {
		return excelContent;
	}

	public void setExcelContent(List<Map<Integer, String>> excelContent) {
		this.excelContent = excelContent;
	}

	public List<String> getExcelHeaders() {
		return excelHeaders;
	}

	public void setExcelHeaders(List<String> excelHeaders) {
		this.excelHeaders = excelHeaders;
	}


	public String getPdfContent() {
		return pdfContent;
	}

	public void setPdfContent(String pdfContent) {
		this.pdfContent = pdfContent;
	}


	private static String convertListToString(List<String> list) {
		StringBuffer sb = new StringBuffer();
		for (String value : list) {
			if (StringUtils.isBlank(value)) {
				continue;
			}
			sb.append(value).append(SEP);
		}
		return sb.toString();
	}

	private static String convertMapToString(Map<Integer, String> map) {
		StringBuffer sb = new StringBuffer();
		for (Map.Entry<Integer, String> entry : map.entrySet()) {
			String value = entry.getValue();
			if (StringUtils.isBlank(value)) {
				continue;
			}
			sb.append(value).append(SEP);
		}
		return sb.toString();
	}

	public static FileBean ofWord(String wordContent) {
		//return ofWord(wordContent);
		FileBean bean = new FileBean();
		bean.setType(TYPE_WORD);
		bean.setWordContent(wordContent);
		return bean;
	}

	public static FileBean ofTxt(String txtContent) {
		FileBean bean = new FileBean();
		bean.setType(TYPE_TXT);
		bean.setWordContent(txtContent);
		return bean;
	}



	public static FileBean ofExcel(List<String> excelHeaders, List<Map<String,String>> excelContentJson) {
		FileBean bean = new FileBean();
		bean.setType(TYPE_EXCEL);
		bean.setExcelHeaders(excelHeaders);
		bean.setExcelContentJson(excelContentJson);
		return bean;
	}
	public static FileBean ofPdf(String pdfContent) {
		FileBean bean = new FileBean();
		bean.setType(TYPE_PDF);
		bean.setPdfContent(pdfContent);
		return bean;
	}

	public static boolean isValidUrl(String urlString) {
		try {
			URL url = new URL(urlString);
			String protocol = url.getProtocol();
			return "http".equalsIgnoreCase(protocol) || "https".equalsIgnoreCase(protocol);
		} catch (MalformedURLException e) {
			return false;
		}
	}
}
