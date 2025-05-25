package com.mkyuan.fountainbase.common.controller.exception;

import java.io.Serializable;

public class QueryDateTooLongException extends RuntimeException implements Serializable {
    private static final long serialVersionUID = 1L;

    public QueryDateTooLongException() {
    }

    public QueryDateTooLongException(String msg) {
        super(msg);
    }

    public QueryDateTooLongException(Throwable cause) {
        super(cause);
    }

    public QueryDateTooLongException(String message, Throwable cause) {
        super(message, cause);
    }
}
