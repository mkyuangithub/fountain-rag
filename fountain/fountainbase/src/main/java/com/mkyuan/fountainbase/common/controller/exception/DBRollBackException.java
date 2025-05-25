package com.mkyuan.fountainbase.common.controller.exception;

import java.io.Serializable;

public class DBRollBackException extends RuntimeException implements Serializable {

	private static final long serialVersionUID = 1L;

	public DBRollBackException() {
	}

	public DBRollBackException(String msg) {
		super(msg);
	}

	public DBRollBackException(Throwable cause) {
		super(cause);
	}

	public DBRollBackException(String message, Throwable cause) {
		super(message, cause);
	}
}
