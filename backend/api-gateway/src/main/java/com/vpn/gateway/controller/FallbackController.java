package com.vpn.gateway.controller;

import com.vpn.common.constant.ErrorCode;
import com.vpn.common.dto.ApiResponse;
import com.vpn.common.dto.ErrorResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Fallback controller для Circuit Breaker
 */
@Slf4j
@RestController
@RequestMapping("/fallback")
public class FallbackController {

    @GetMapping
    public ApiResponse<Void> fallback() {
        log.warn("Circuit breaker fallback triggered");

        ErrorResponse errorResponse = ErrorResponse.builder()
                .code(ErrorCode.SERVER_UNAVAILABLE.getCode())
                .message("Service is temporarily unavailable. Please try again later.")
                .build();

        return ApiResponse.error(errorResponse);
    }
}
