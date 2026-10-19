package org.whiteicetea.warehousemanagementsystem.exception;


import org.whiteicetea.warehousemanagementsystem.common.ErrorCode;

public class GoodsNotFoundException extends BusinessException {
    public GoodsNotFoundException(String message) {
        super(ErrorCode.GOODS_NOT_FOUND, message);
    }



}
