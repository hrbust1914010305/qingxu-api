package com.qingxu.qingxuapi.common.exception;

import com.qingxu.qingxuapi.common.response.ApiResponse;
import com.qingxu.qingxuapi.common.response.ErrorCode;
import com.qingxu.qingxuapi.common.response.ResponseFactory;
import jakarta.validation.ConstraintViolationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.NoHandlerFoundException;

@Slf4j
@RestControllerAdvice
@RequiredArgsConstructor
public class GlobalExceptionHandler {

    private final ResponseFactory responseFactory;

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse<Void>> handleBusinessException(BusinessException exception) {
        log.warn("业务异常: code={}, message={}", exception.getErrorCode().getCode(), exception.getMessage());
        return ResponseEntity
                .status(exception.getErrorCode().getHttpStatus())
                .body(responseFactory.failure(exception.getErrorCode(), exception.getMessage()));
    }

    @ExceptionHandler(UnauthorizedException.class)
    public ResponseEntity<ApiResponse<Void>> handleUnauthorizedException(UnauthorizedException exception) {
        log.warn("未授权访问");
        return ResponseEntity
                .status(ErrorCode.AUTH_401.getHttpStatus())
                .body(responseFactory.failure(ErrorCode.AUTH_401));
    }

    @ExceptionHandler(NoHandlerFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleNoHandlerFoundException(NoHandlerFoundException exception) {
        log.warn("接口不存在: {} {}", exception.getHttpMethod(), exception.getRequestURL());
        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(responseFactory.failure(ErrorCode.NOT_FOUND));
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ApiResponse<Void>> handleHttpRequestMethodNotSupportedException(HttpRequestMethodNotSupportedException exception) {
        log.warn("请求方法不允许: {}", exception.getMethod());
        String message = String.format("请求方法 %s 不允许，请使用正确的请求方式（GET/POST/PUT/DELETE）", exception.getMethod());
        return ResponseEntity
                .status(HttpStatus.METHOD_NOT_ALLOWED)
                .body(responseFactory.failure(ErrorCode.METHOD_NOT_ALLOWED, message));
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<ApiResponse<Void>> handleHttpMediaTypeNotSupportedException(HttpMediaTypeNotSupportedException exception) {
        log.warn("不支持的媒体类型: {}", exception.getContentType());
        return ResponseEntity
                .status(HttpStatus.UNSUPPORTED_MEDIA_TYPE)
                .body(responseFactory.failure(ErrorCode.MEDIA_TYPE_NOT_SUPPORTED));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiResponse<Void>> handleHttpMessageNotReadableException(HttpMessageNotReadableException exception) {
        log.warn("请求体格式错误: 请求数据无法解析");
        return ResponseEntity
                .badRequest()
                .body(responseFactory.failure(ErrorCode.REQUEST_BODY_INVALID));
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ApiResponse<Void>> handleMissingServletRequestParameterException(MissingServletRequestParameterException exception) {
        log.warn("缺少参数: {}", exception.getParameterName());
        String message = String.format("缺少必填参数【%s】，类型：%s", exception.getParameterName(), exception.getParameterType());
        return ResponseEntity
                .badRequest()
                .body(responseFactory.failure(ErrorCode.MISSING_PARAMETER, message));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleMethodArgumentNotValidException(MethodArgumentNotValidException exception) {
        String errorMessage = exception.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .reduce((msg1, msg2) -> msg1 + "; " + msg2)
                .orElse("参数校验失败");

        log.warn("参数校验失败: {}", errorMessage);
        return ResponseEntity
                .badRequest()
                .body(responseFactory.failure(ErrorCode.VALIDATION_ERROR, errorMessage));
    }

    @ExceptionHandler(BindException.class)
    public ResponseEntity<ApiResponse<Void>> handleBindException(BindException exception) {
        String errorMessage = exception.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .reduce((msg1, msg2) -> msg1 + "; " + msg2)
                .orElse("参数校验失败");

        log.warn("参数绑定失败: {}", errorMessage);
        return ResponseEntity
                .badRequest()
                .body(responseFactory.failure(ErrorCode.VALIDATION_ERROR, errorMessage));
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiResponse<Void>> handleConstraintViolationException(ConstraintViolationException exception) {
        String errorMessage = exception.getConstraintViolations().stream()
                .map(violation -> violation.getPropertyPath() + ": " + violation.getMessage())
                .reduce((msg1, msg2) -> msg1 + "; " + msg2)
                .orElse("参数校验失败");

        log.warn("约束校验失败: {}", errorMessage);
        return ResponseEntity
                .badRequest()
                .body(responseFactory.failure(ErrorCode.VALIDATION_ERROR, errorMessage));
    }

    @ExceptionHandler(DuplicateKeyException.class)
    public ResponseEntity<ApiResponse<Void>> handleDuplicateKeyException(DuplicateKeyException exception) {
        log.warn("数据重复: {}", exception.getMessage());
        return ResponseEntity
                .badRequest()
                .body(responseFactory.failure(ErrorCode.VALIDATION_ERROR, "数据已存在，请检查用户名、邮箱或手机号是否重复"));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleException(Exception exception) {
        log.error("系统异常: {} - {}", exception.getClass().getSimpleName(), exception.getMessage(), exception);
        return ResponseEntity
                .internalServerError()
                .body(responseFactory.failure(ErrorCode.SYSTEM_ERROR));
    }
}