package com.codepliot.exception;

import com.codepliot.model.ErrorCode;
import lombok.Getter;
/**
 * BusinessException 异常类，用于表达业务或系统错误。
 */
@Getter
public class BusinessException extends RuntimeException {

    private final Integer code;
/**
 * 创建 BusinessException 实例。
 */
public BusinessException(String message) {
        super(message);
        this.code = ErrorCode.BAD_REQUEST.getCode();
    }
/**
 * 创建 BusinessException 实例。
 */
public BusinessException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.code = errorCode.getCode();
    }
/**
 * 创建 BusinessException 实例。
 */
public BusinessException(ErrorCode errorCode, String message) {
        super(message);
        this.code = errorCode.getCode();
    }
}

