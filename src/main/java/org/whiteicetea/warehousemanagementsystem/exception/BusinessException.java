package org.whiteicetea.warehousemanagementsystem.exception;

import org.whiteicetea.warehousemanagementsystem.common.ErrorCode;

public class BusinessException extends RuntimeException {

    private ErrorCode errorCode;

    public BusinessException(ErrorCode errorCode,String message) {
        super(message);
        this.errorCode = errorCode;

    }
    public ErrorCode getErrorCode() {
        return errorCode;
    }

}
