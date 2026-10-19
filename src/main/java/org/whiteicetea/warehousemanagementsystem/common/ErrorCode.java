package org.whiteicetea.warehousemanagementsystem.common;

import org.springframework.http.HttpStatus;

public enum ErrorCode {
    GOODS_NOT_FOUND(HttpStatus.NOT_FOUND);

    private final HttpStatus Status;

    ErrorCode(HttpStatus status) {
        this.Status = status;
    }

    public HttpStatus getStatus() {
        return Status;
    }
}
