package com.vpn.common.exception;

import com.vpn.common.constant.ErrorCode;

public class ValidationException extends BaseException {
    public ValidationException(String message) {
        super(ErrorCode.INVALID_REQUEST, message);
    }
}
