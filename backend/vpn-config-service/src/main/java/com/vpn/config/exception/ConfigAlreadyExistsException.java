package com.vpn.config.exception;

import com.vpn.common.constant.ErrorCode;
import com.vpn.common.exception.BaseException;

public class ConfigAlreadyExistsException extends BaseException {

    public ConfigAlreadyExistsException(Long deviceId) {
        super(ErrorCode.CONFIG_ALREADY_EXISTS,
                "Active configuration already exists for device: " + deviceId);
    }
}
