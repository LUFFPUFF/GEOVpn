package com.vpn.user.exception;

import com.vpn.common.constant.ErrorCode;
import com.vpn.common.exception.BaseException;

public class UserNotFoundException extends BaseException {

    public UserNotFoundException(Long telegramId) {
        super(ErrorCode.USER_NOT_FOUND,
                "User not found with Telegram ID: " + telegramId);
    }

    public UserNotFoundException(String message) {
        super(ErrorCode.USER_NOT_FOUND, message);
    }
}
