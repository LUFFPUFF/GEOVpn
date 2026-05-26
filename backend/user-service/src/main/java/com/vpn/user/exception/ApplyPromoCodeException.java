package com.vpn.user.exception;

import com.vpn.common.constant.ErrorCode;
import com.vpn.common.exception.BaseException;

import java.util.UUID;

public class ApplyPromoCodeException extends BaseException {

    public ApplyPromoCodeException(UUID uuid) {
        super(ErrorCode.APPLY_PROMO_CODE,
                "Device not found with UUID: " + uuid);
    }

    public ApplyPromoCodeException(String message) {
        super(ErrorCode.APPLY_PROMO_CODE, message);
    }
}
