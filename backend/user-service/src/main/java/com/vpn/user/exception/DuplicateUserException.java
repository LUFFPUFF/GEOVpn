package com.vpn.user.exception;

import com.vpn.common.constant.ErrorCode;
import com.vpn.common.exception.BaseException;

public class DuplicateUserException extends BaseException {

    public DuplicateUserException(Long telegramId) {
        super(ErrorCode.DUPLICATE_USER,
                "User already exists with Telegram ID: " + telegramId);
    }
}
