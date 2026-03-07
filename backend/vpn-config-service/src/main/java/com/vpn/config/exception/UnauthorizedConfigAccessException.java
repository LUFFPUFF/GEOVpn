package com.vpn.config.exception;

import com.vpn.common.constant.ErrorCode;
import com.vpn.common.exception.BaseException;

public class UnauthorizedConfigAccessException extends BaseException {

    public UnauthorizedConfigAccessException(Long deviceId, Long userId) {
        super(ErrorCode.FORBIDDEN,
                String.format("User %d is not authorized to access config for device %d",
                        userId, deviceId));
    }
}
