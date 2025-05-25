package com.mkyuan.fountainbase.office.support;

//import com.mkyuan.fountainbase.AgentHelper;
import com.mkyuan.fountainbase.common.util.okhttp.OkHttpHelper;
import com.mkyuan.fountainbase.office.FileBean;
import com.mkyuan.fountainbase.office.FileResolver;
import okhttp3.HttpUrl;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public abstract class AbstractFileResolver implements FileResolver {
	protected Logger logger = LogManager.getLogger(this.getClass());
	protected final int maxReadLength;

	//protected AgentHelper agentHelper;

	protected String token="";
	protected String userName="";
	protected boolean readImg=false;
	protected String apiKey="";
	protected OkHttpHelper okHttpHelper;
	protected String ocrUrl="";


	public String getOcrUrl() {
		return ocrUrl;
	}

	public void setOcrUrl(String ocrUrl) {
		this.ocrUrl = ocrUrl;
	}

	public String getApiKey() {
		return apiKey;
	}

	public void setApiKey(String apiKey) {
		this.apiKey = apiKey;
	}

	public OkHttpHelper getOkHttpHelper() {
		return okHttpHelper;
	}

	public void setOkHttpHelper(OkHttpHelper okHttpHelper) {
		this.okHttpHelper = okHttpHelper;
	}

	public boolean isReadImg() {
		return readImg;
	}

	public void setReadImg(boolean readImg) {
		this.readImg = readImg;
	}

	public String getToken() {
		return token;
	}

	public void setToken(String token) {
		this.token = token;
	}

	public String getUserName() {
		return userName;
	}

	public void setUserName(String userName) {
		this.userName = userName;
	}

	protected AbstractFileResolver(int maxReadLength,boolean readImg,String apiKey, OkHttpHelper okHttpHelper,String ocrUrl)
	{
		this.maxReadLength = maxReadLength;
		this.readImg=readImg;
		this.apiKey=apiKey;
		this.okHttpHelper=okHttpHelper;
		this.ocrUrl=ocrUrl;
	}
	protected String sendImageToOcr(OkHttpHelper okHttpHelper,String apiKey, byte[] pictureData) {
		String contentText="";
		try{
			Map<String, String> headers = new HashMap<>();
			headers.put("Content-Type", "application/json");
			headers.put("apiKey", apiKey);

			String jsonResponse = okHttpHelper.postImageWithHeaders(ocrUrl,pictureData,headers,new HashMap<>());
			contentText=this.extractOcrText(jsonResponse);

		}catch(Exception e){
			logger.error(">>>>>>处理图片失败->{}",e.getMessage(),e);
		}
		return contentText;
	}
	/**
	 * 处理OCR服务返回的JSON结果
	 * @param jsonResponse OCR服务返回的JSON字符串
	 * @return 提取的文本内容，每个文本后追加换行符
	 */
	private String extractOcrText(String jsonResponse) {
		try {
			StringBuilder textBuilder = new StringBuilder();

			// 解析JSON
			JSONObject responseObj = new JSONObject(jsonResponse);

			// 获取results数组
			if (responseObj.has("results")) {
				JSONArray resultsArray = responseObj.getJSONArray("results");

				// 遍历results数组
				for (int i = 0; i < resultsArray.length(); i++) {
					JSONObject resultObj = resultsArray.getJSONObject(i);

					// 提取text字段
					if (resultObj.has("text")) {
						String text = resultObj.getString("text");
						textBuilder.append(text).append("\n");
					}
				}
			}

			return textBuilder.toString();
		} catch (Exception e) {
			logger.error(">>>>>>解析OCR结果失败->{}", e.getMessage(),e);
			return "";
		}
	}
	@Override
	public FileBean resolve(String fileName, byte[] fileData) throws IOException {
		if (StringUtils.isBlank(fileName)) {
			throw new IOException("文件名称为空");
		}
		if (null == fileData) {
			throw new IOException("文件内容为空");
		}
		try {
			return this.doResolve(fileName, fileData);
		} catch (IOException e) {
			throw e;
		} catch (Throwable e) {
			throw new IOException(e.getMessage(), e);
		}
	}

	public int getMaxReadLength() {
		return maxReadLength;
	}

	/*
	public void setAgentHelper(AgentHelper agentHelper) {
		this.agentHelper = agentHelper;
	}
	*/
	protected abstract FileBean doResolve(String fileName, byte[] fileData) throws IOException;


}
