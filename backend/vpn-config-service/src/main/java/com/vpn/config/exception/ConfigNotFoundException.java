package com.vpn.config.exception;

import com.vpn.common.constant.ErrorCode;
import com.vpn.common.exception.BaseException;

public class ConfigNotFoundException extends BaseException {

    public ConfigNotFoundException(Long deviceId) {
        super(ErrorCode.CONFIG_NOT_FOUND,
                "Configuration not found for device: " + deviceId);
    }

    public ConfigNotFoundException(String message) {
        super(ErrorCode.CONFIG_NOT_FOUND, message);
    }
}
