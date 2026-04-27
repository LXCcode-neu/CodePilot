package com.codepliot.handler;

import com.codepliot.exception.BusinessException;

import com.codepliot.model.ErrorCode;
import com.codepliot.model.Result;
import jakarta.validation.ConstraintViolationException;
import java.util.stream.Collectors;
import org.mybatis.spring.MyBatisSystemException;
import org.springframework.validation.BindException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
/**
 * GlobalExceptionHandler 处理器，负责统一处理横切逻辑。
 */
@RestControllerAdvice
public class GlobalExceptionHandler {
    /**
     * 处理Business Exception相关逻辑。
     */
@ExceptionHandler(BusinessException.class)
    public Result<Void> handleBusinessException(BusinessException exception) {
        return new Result<>(exception.getCode(), exception.getMessage(), null);
    }
    /**
     * 处理Method Argument Not Valid Exception相关逻辑。
     */
@ExceptionHandler(MethodArgumentNotValidException.class)
    public Result<Void> handleMethodArgumentNotValidException(MethodArgumentNotValidException exception) {
        String message = exception.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .collect(Collectors.joining("; "));
        return Result.failure(ErrorCode.VALIDATION_ERROR, message);
    }
    /**
     * 处理Bind Exception相关逻辑。
     */
@ExceptionHandler(BindException.class)
    public Result<Void> handleBindException(BindException exception) {
        String message = exception.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .collect(Collectors.joining("; "));
        return Result.failure(ErrorCode.VALIDATION_ERROR, message);
    }
    /**
     * 处理Constraint Violation Exception相关逻辑。
     */
@ExceptionHandler(ConstraintViolationException.class)
    public Result<Void> handleConstraintViolationException(ConstraintViolationException exception) {
        return Result.failure(ErrorCode.VALIDATION_ERROR, exception.getMessage());
    }
    /**
     * 处理My Batis System Exception相关逻辑。
     */
@ExceptionHandler(MyBatisSystemException.class)
    public Result<Void> handleMyBatisSystemException(MyBatisSystemException exception) {
        Throwable rootCause = getRootCause(exception);
        String message = rootCause.getMessage();
        return Result.failure(ErrorCode.INTERNAL_ERROR, message == null ? "Database access error" : message);
    }
    /**
     * 处理Exception相关逻辑。
     */
@ExceptionHandler(Exception.class)
    public Result<Void> handleException(Exception exception) {
        Throwable rootCause = getRootCause(exception);
        String message = rootCause.getMessage();
        return Result.failure(ErrorCode.INTERNAL_ERROR, message == null ? exception.toString() : message);
    }
/**
 * 获取Root Cause相关逻辑。
 */
private Throwable getRootCause(Throwable throwable) {
        Throwable current = throwable;
        while (current.getCause() != null && current.getCause() != current) {
            current = current.getCause();
        }
        return current;
    }
}



