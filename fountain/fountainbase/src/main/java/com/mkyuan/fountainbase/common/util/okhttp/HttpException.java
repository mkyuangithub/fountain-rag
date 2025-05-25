package com.mkyuan.fountainbase.common.util.okhttp;

import java.io.Serializable;

public class HttpException extends RuntimeException implements Serializable {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public HttpException() {
	}

	public HttpException(String msg) {
		super(msg);
	}

	public HttpException(Throwable cause) {
		super(cause);
	}

	public HttpException(String message, Throwable cause) {
		super(message, cause);
	}
}
