package com.vpn.common.exception;

import com.vpn.common.constant.ErrorCode;

public class UnauthorizedException extends BaseException {

    public UnauthorizedException(String message) {
        super(ErrorCode.UNAUTHORIZED, message);
    }

    public UnauthorizedException() {
        super(ErrorCode.UNAUTHORIZED, "Authentication required");
    }
}
