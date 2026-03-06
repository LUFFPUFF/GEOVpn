package com.vpn.user.exception;

import com.vpn.common.constant.AppConstants;
import com.vpn.common.constant.ErrorCode;
import com.vpn.common.exception.BaseException;

public class MaxDevicesExceededException extends BaseException {

    public MaxDevicesExceededException(Long userId) {
        super(ErrorCode.MAX_DEVICES_EXCEEDED,
                String.format("Maximum number of devices (%d) exceeded for user: %d",
                        AppConstants.MAX_DEVICES_PER_USER, userId));
    }
}
