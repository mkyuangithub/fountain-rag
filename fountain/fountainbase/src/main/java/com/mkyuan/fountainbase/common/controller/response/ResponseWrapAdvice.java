package com.mkyuan.fountainbase.common.controller.response;

import java.lang.reflect.Method;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.serializer.SerializerFeature;
import com.mkyuan.fountainbase.common.controller.exception.*;


/**
 * 全局异常处理器
 */
@Component
@RestControllerAdvice
public class ResponseWrapAdvice implements ResponseBodyAdvice<Object> {

	protected Logger logger = LogManager.getLogger(this.getClass());



	@ExceptionHandler(LoginFailedException.class)
	public Object loginFailed(LoginFailedException e) {
		return new ResponseBean(ResponseCodeEnum.LOGIN_ERROR, e.getMessage());
	}

	@ExceptionHandler(IllegalStateException.class)
	public ResponseBean illegalStateException(IllegalStateException e) {
		logger.error(">>>>>> exception handler ->{}", e.getMessage(), e);
		return new ResponseBean(ResponseCodeEnum.FAIL.getCode(), e.getMessage());
	}

	/**
	 * 处理其它异常
	 */
	@ExceptionHandler(Exception.class)
	public ResponseBean handleAllException(Exception e) {
		logger.error(">>>>>> exception handler ->{}", e.getMessage(), e);
		return new ResponseBean(ResponseCodeEnum.FAIL.getCode(), e.getMessage());
	}

	@Override
	public boolean supports(MethodParameter returnType, Class<? extends HttpMessageConverter<?>> converterType) {
		return true;
	}

	@Override
	public Object beforeBodyWrite(Object body, MethodParameter returnType, MediaType selectedContentType, Class<? extends HttpMessageConverter<?>> selectedConverterType, ServerHttpRequest request, ServerHttpResponse response) {
		String path = request.getURI().getPath();
		if (null != path && path.startsWith("/actuator") ) {
			return body;
		}
		if("/error".equals(path)) {
			return body;
		}
		ResponseIgnore ignore = returnType.getMethodAnnotation(ResponseIgnore.class);
		if (null != ignore) {
			return body;
		}
		if (null != body && body instanceof ResponseBean) {
			return body;
		}
		Method method = returnType.getMethod();
		if (String.class.equals(method.getReturnType())) {
			response.getHeaders().set(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);
			return JSON.toJSONString(ResponseBean.success(body), SerializerFeature.WriteMapNullValue);
		}
		return ResponseBean.success(body);
	}

}