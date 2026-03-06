package com.vpn.config.exception;

import com.vpn.common.constant.ErrorCode;
import com.vpn.common.exception.BaseException;

public class NoAvailableServerException extends BaseException {

    public NoAvailableServerException() {
        super(ErrorCode.NO_AVAILABLE_SERVERS, "No available VPN servers found");
    }

    public NoAvailableServerException(String message) {
        super(ErrorCode.NO_AVAILABLE_SERVERS, message);
    }
}
