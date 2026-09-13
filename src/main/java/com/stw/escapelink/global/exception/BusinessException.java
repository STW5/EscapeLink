package com.stw.escapelink.global.exception;

import java.util.Map;

public class BusinessException extends RuntimeException {

    private final ErrorCode errorCode;
    private final Map<String, Object> data;

    public BusinessException(ErrorCode errorCode) {
        this(errorCode, errorCode.getDefaultMessage(), Map.of());
    }

    public BusinessException(ErrorCode errorCode, Map<String, Object> data) {
        this(errorCode, errorCode.getDefaultMessage(), data);
    }

    public BusinessException(ErrorCode errorCode, String message, Map<String, Object> data) {
        super(message);
        this.errorCode = errorCode;
        this.data = data;
    }

    public ErrorCode getErrorCode() {
        return errorCode;
    }

    public Map<String, Object> getData() {
        return data;
    }
}
