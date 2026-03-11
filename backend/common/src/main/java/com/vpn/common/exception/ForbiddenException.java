package com.vpn.common.exception;

import com.vpn.common.constant.ErrorCode;
import com.vpn.common.security.UserRole;

public class ForbiddenException extends BaseException {

    public ForbiddenException(String message) {
        super(ErrorCode.FORBIDDEN, message);
    }

    public ForbiddenException(UserRole required, UserRole actual) {
        super(ErrorCode.FORBIDDEN,
                String.format("Access denied. Required role: %s, actual: %s", required, actual));
    }

    public ForbiddenException() {
        super(ErrorCode.FORBIDDEN, "You don't have permission to access this resource");
    }
}
