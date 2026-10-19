package org.whiteicetea.warehousemanagementsystem.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

public class GoodsNotFoundException extends RuntimeException {
    public GoodsNotFoundException(String message) {
        super(message);
    }
    @RestControllerAdvice
    public class GlobalExceptionHandler{
        @ExceptionHandler(GoodsNotFoundException.class)
        public ResponseEntity<String> handleGoodsNotFound(GoodsNotFoundException e){
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        }
    }


}
