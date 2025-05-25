/**
 * 系统项目名称 org.sky.edger.util.common.response ResponseBean.java
 * 
 * Aug 6, 2021-1:58:32 PM 2021XX公司-版权所有
 * 
 */
package com.mkyuan.fountainbase.common.controller.response;

import java.io.Serializable;

import org.apache.commons.lang3.StringUtils;


/**
 * 
 * ResponseBean
 * 
 * 
 * Aug 6, 2021 1:58:32 PM
 * 
 * @version 1.0.0
 * 
 */
public class ResponseBean implements Serializable {
	private static final long serialVersionUID = -6168385396584895333L;

	private Integer code;

	private String message;

	private Object data;

	public ResponseBean() {
	}

	public ResponseBean(ResponseCodeEnum codeEnum) {
		this.code = codeEnum.getCode();
		this.message = codeEnum.getMessage();
	}

	public ResponseBean(Integer code, String message) {
		this.code = code;
		this.message = message;
	}

	public ResponseBean(ResponseCodeEnum codeEnum, Object data) {
		this.code = codeEnum.getCode();
		this.message = codeEnum.getMessage();
		this.data = data;
	}

	public ResponseBean(Integer code, String message, Object data) {
		this.code = code;
		this.data = data;
		this.message = message;
	}

	public Integer getCode() {
		return code;
	}

	public void setCode(Integer code) {
		this.code = code;
	}

	public String getMessage() {
		return message;
	}

	public void setMessage(String message) {
		this.message = message;
	}

	public Object getData() {
		return data;
	}

	public void setData(Object data) {
		this.data = data;
	}

	public static ResponseBean success(Object data, String message) {
		ResponseBean result = new ResponseBean();
		result.setMessage(StringUtils.isBlank(message) ? ResponseCodeEnum.SUCCESS.getMessage() : message);
		result.setData(data);
		result.setCode(ResponseCodeEnum.SUCCESS.getCode());
		return result;
	}

	public static ResponseBean success(Object data) {
		return success(data, null);
	}

	public static ResponseBean success() {
		return success(null, null);
	}



}
