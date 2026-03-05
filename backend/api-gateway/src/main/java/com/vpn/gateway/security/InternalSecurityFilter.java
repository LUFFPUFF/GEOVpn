package com.vpn.gateway.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vpn.common.constant.ErrorCode;
import com.vpn.common.dto.ErrorResponse;
import com.vpn.common.util.StringUtils;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Фильтр для микросервисов.
 * Гарантирует, что запрос пришел от API Gateway, а не напрямую извне.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class InternalSecurityFilter extends OncePerRequestFilter {

    @Value("${service.security.internal-secret}")
    private String expectedInternalSecret;

    private final ObjectMapper objectMapper;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain) throws ServletException, IOException {

        if (request.getRequestURI().startsWith("/actuator/")) {
            filterChain.doFilter(request, response);
            return;
        }

        String actualSecret = request.getHeader("X-Internal-Secret");

        if (actualSecret == null || !actualSecret.equals(expectedInternalSecret)) {
            log.warn("Blocked direct access attempt to internal microservice. IP: {}, URI: {}",
                    request.getRemoteAddr(), request.getRequestURI());

            sendUnauthorizedResponse(response);
            return;
        }

        filterChain.doFilter(request, response);
    }

    private void sendUnauthorizedResponse(HttpServletResponse response) throws IOException {
        response.setStatus(HttpStatus.FORBIDDEN.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);

        ErrorResponse errorResponse = ErrorResponse.builder()
                .code(ErrorCode.FORBIDDEN.getCode())
                .message("Direct access to internal microservice is forbidden")
                .traceId(StringUtils.generateUuid())
                .build();

        objectMapper.writeValue(response.getWriter(), errorResponse);
    }
}
