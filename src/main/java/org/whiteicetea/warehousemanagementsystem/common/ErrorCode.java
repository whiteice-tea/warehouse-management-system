package org.whiteicetea.warehousemanagementsystem.common;

import org.springframework.http.HttpStatus;

public enum ErrorCode {
    GOODS_NOT_FOUND(HttpStatus.NOT_FOUND),
    INVALID_ARGUMENT(HttpStatus.BAD_REQUEST),
    METHOD_NOT_ALLOWED(HttpStatus.METHOD_NOT_ALLOWED),
    UNSUPPORTED_MEDIA_TYPE(HttpStatus.UNSUPPORTED_MEDIA_TYPE),
    INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR);

    private final HttpStatus Status;

    ErrorCode(HttpStatus status) {
        this.Status = status;
    }

    public HttpStatus getStatus() {
        return Status;
    }
}
