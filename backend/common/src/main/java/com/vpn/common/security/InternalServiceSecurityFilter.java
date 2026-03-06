package com.vpn.common.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vpn.common.constant.ErrorCode;
import com.vpn.common.dto.ApiResponse;
import com.vpn.common.dto.ErrorResponse;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * Фильтр для микросервисов (НЕ для Gateway!)
 * Проверяет что запрос пришел от Gateway
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(
        name = "service.security.internal-check-enabled",
        havingValue = "true",
        matchIfMissing = true
)
public class InternalServiceSecurityFilter extends OncePerRequestFilter{

    @Value("${service.security.internal-secret}")
    private String expectedSecret;

    private final ObjectMapper objectMapper;

    private static final List<String> WHITELISTED_PATHS = List.of(
            "/actuator/",
            "/error"
    );

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain) throws ServletException, IOException {

        String requestUri = request.getRequestURI();

        if (WHITELISTED_PATHS.stream().anyMatch(requestUri::startsWith)) {
            filterChain.doFilter(request, response);
            return;
        }

        String actualSecret = request.getHeader("X-Internal-Secret");

        if (actualSecret == null || !actualSecret.equals(expectedSecret)) {
            log.warn("Blocked direct access attempt. IP: {}, URI: {}, Secret: {}",
                    request.getRemoteAddr(),
                    requestUri,
                    actualSecret != null ? "invalid" : "missing");

            sendForbiddenResponse(response);
            return;
        }

        log.debug("Internal security check passed for: {}", requestUri);
        filterChain.doFilter(request, response);
    }

    private void sendForbiddenResponse(HttpServletResponse response) throws IOException {
        response.setStatus(HttpStatus.FORBIDDEN.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);

        ErrorResponse errorResponse = ErrorResponse.builder()
                .code(ErrorCode.FORBIDDEN.getCode())
                .message("Direct access to internal microservice is forbidden. Please use API Gateway.")
                .build();

        ApiResponse<Void> apiResponse = ApiResponse.error(errorResponse);
        objectMapper.writeValue(response.getWriter(), apiResponse);
    }
}
