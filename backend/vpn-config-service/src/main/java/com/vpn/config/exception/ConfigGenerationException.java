package com.vpn.config.exception;

import com.vpn.common.constant.ErrorCode;
import com.vpn.common.exception.BaseException;

public class ConfigGenerationException extends BaseException {

    public ConfigGenerationException(String message) {
        super(ErrorCode.CONFIG_GENERATION_FAILED, message);
    }

    public ConfigGenerationException(String message, Throwable cause) {
        super(ErrorCode.CONFIG_GENERATION_FAILED, message);
        initCause(cause);
    }
}
