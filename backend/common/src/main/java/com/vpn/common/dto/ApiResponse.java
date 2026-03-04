package com.vpn.common.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.slf4j.MDC;

/**
 * Для возврата ответов от API
 * @param <T> принимаемый content
 */

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse<T> {

    private boolean success;
    private T data;
    private ErrorResponse error;

    public static <T> ApiResponse<T> success(T data) {
        return ApiResponse.<T>builder()
                .success(true)
                .data(data)
                .build();
    }

    public static <T> ApiResponse<T> error(ErrorResponse errorResponse) {
        if (errorResponse.getTraceId() == null) {
            errorResponse.setTraceId(MDC.get("traceId"));
        }
        return ApiResponse.<T>builder()
                .success(false)
                .error(errorResponse)
                .build();
    }
}
