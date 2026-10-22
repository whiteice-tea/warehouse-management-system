package org.whiteicetea.warehousemanagementsystem.exception;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.whiteicetea.warehousemanagementsystem.common.ApiResponse;

@RestControllerAdvice
    public class GlobalExceptionHandler{

        @ExceptionHandler(BusinessException.class)
        public ResponseEntity<ApiResponse<Void>> handleBusinessException(BusinessException e){
            return ResponseEntity.status(e.getErrorCode().getStatus()).body(new ApiResponse<Void>(
                    e.getErrorCode().name(),
                    e.getMessage(),
                    null
            ));
        }
    }