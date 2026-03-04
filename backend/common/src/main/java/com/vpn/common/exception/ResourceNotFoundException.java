package com.vpn.common.exception;

import com.vpn.common.constant.ErrorCode;

public class ResourceNotFoundException extends BaseException {
    public ResourceNotFoundException(String resourceName, String fieldName, Object fieldValue) {
        super(ErrorCode.INVALID_REQUEST, String.format("%s not found with %s : '%s'", resourceName, fieldName, fieldValue));
    }

    public ResourceNotFoundException(String message) {
        super(ErrorCode.INVALID_REQUEST, message);
    }
}
