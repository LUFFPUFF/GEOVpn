package com.vpn.user.exception;

import com.vpn.common.constant.ErrorCode;
import com.vpn.common.exception.BaseException;

import java.util.UUID;

public class DeviceNotFoundException extends BaseException {

    public DeviceNotFoundException(UUID uuid) {
        super(ErrorCode.DEVICE_NOT_FOUND,
                "Device not found with UUID: " + uuid);
    }

    public DeviceNotFoundException(String message) {
        super(ErrorCode.DEVICE_NOT_FOUND, message);
    }
}
