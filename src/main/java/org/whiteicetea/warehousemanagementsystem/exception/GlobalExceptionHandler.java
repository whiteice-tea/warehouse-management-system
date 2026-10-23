package org.whiteicetea.warehousemanagementsystem.exception;

import org.springframework.beans.TypeMismatchException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.ErrorResponse;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.whiteicetea.warehousemanagementsystem.common.ApiResponse;
import org.whiteicetea.warehousemanagementsystem.common.ErrorCode;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RestControllerAdvice
public class GlobalExceptionHandler{
    private static final Logger log =
            LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse<Void>> handleBusinessException(BusinessException e){
        return ResponseEntity.status(e.getErrorCode().getStatus()).body(new ApiResponse<Void>(
                e.getErrorCode().name(),
                e.getMessage(),
                null
        ));
    }
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiResponse<Void>> handleHttpMessageNotReadableException(HttpMessageNotReadableException e){
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ApiResponse<Void>(
                ErrorCode.INVALID_ARGUMENT.name(),
                "请求体格式错误",
                null
        ));
    }
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ApiResponse<Void>> handleHttpRequestMethodNotSupportedException(HttpRequestMethodNotSupportedException e){
        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED).body(new ApiResponse<Void>(
                ErrorCode.METHOD_NOT_ALLOWED.name(),
                "请求方法不支持",
                null
        ));
    }
    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<ApiResponse<Void>> handleHttpMediaTypeNotSupportedException(HttpMediaTypeNotSupportedException e){
        return ResponseEntity.status(HttpStatus.UNSUPPORTED_MEDIA_TYPE).body(new ApiResponse<Void>(
                ErrorCode.UNSUPPORTED_MEDIA_TYPE.name(),
                "请求内容类型不支持",
                null
        ));

    }
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleException(Exception e) throws Exception{

        if (e instanceof ErrorResponse || e instanceof TypeMismatchException) {
            throw e;
        }
        log.error("发生未预料的服务异常",e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new ApiResponse<Void>(
                ErrorCode.INTERNAL_ERROR.name(),
                "服务器内部错误",
                null
        ));

    }


}