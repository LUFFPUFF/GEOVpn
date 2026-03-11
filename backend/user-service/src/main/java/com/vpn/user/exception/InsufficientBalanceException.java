package com.vpn.user.exception;

import com.vpn.common.constant.ErrorCode;
import com.vpn.common.exception.BaseException;

public class InsufficientBalanceException extends BaseException {
    public InsufficientBalanceException(Integer required, Integer available) {
        super(ErrorCode.INSUFFICIENT_BALANCE,
                String.format("Insufficient balance. Required: %d, Available: %d",
                        required, available));
    }
}
