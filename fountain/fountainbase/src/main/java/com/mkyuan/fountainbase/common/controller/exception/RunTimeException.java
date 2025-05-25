package com.mkyuan.fountainbase.common.controller.exception;

import java.io.Serializable;

public class RunTimeException extends RuntimeException implements Serializable {

	private static final long serialVersionUID = 1L;

	public RunTimeException() {
	}

	public RunTimeException(String msg) {
		super(msg);
	}

	public RunTimeException(Throwable cause) {
		super(cause);
	}

	public RunTimeException(String message, Throwable cause) {
		super(message, cause);
	}

}
