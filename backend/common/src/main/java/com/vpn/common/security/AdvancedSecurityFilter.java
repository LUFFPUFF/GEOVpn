package com.vpn.common.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vpn.common.constant.ErrorCode;
import com.vpn.common.constant.Role;
import com.vpn.common.dto.ApiResponse;
import com.vpn.common.dto.ErrorResponse;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class AdvancedSecurityFilter extends OncePerRequestFilter {

    private final SecurityProperties securityProperties;
    private final ObjectMapper objectMapper;

    private static final List<String> WHITELISTED_PATHS = List.of("/actuator/", "/error"); //todo дополнить список белых списков

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
        if (actualSecret == null || !actualSecret.equals(securityProperties.getInternalSecret())) {
            log.warn("Blocked direct access attempt. IP: {}, URI: {}", request.getRemoteAddr(), requestUri);
            sendForbiddenResponse(response);
            return;
        }

        List<GrantedAuthority> authorities = new ArrayList<>();

        authorities.add(new SimpleGrantedAuthority(Role.SERVICE.getValue()));

        String userIdHeader = request.getHeader("X-User-Id");
        String principal = "SERVICE_ACCOUNT";

        if (userIdHeader != null && !userIdHeader.isBlank()) {
            try {
                Long telegramId = Long.parseLong(userIdHeader);
                principal = String.valueOf(telegramId);

                authorities.add(new SimpleGrantedAuthority(Role.USER.getValue()));

                if (securityProperties.getAdmins().contains(telegramId)) {
                    authorities.add(new SimpleGrantedAuthority(Role.ADMIN.getValue()));
                }
            } catch (NumberFormatException e) {
                log.warn("Invalid X-User-Id format: {}", userIdHeader);
            }
        }

        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(principal, null, authorities);
        authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

        SecurityContextHolder.getContext().setAuthentication(authentication);

        filterChain.doFilter(request, response);
    }

    private void sendForbiddenResponse(HttpServletResponse response) throws IOException {
        response.setStatus(HttpStatus.FORBIDDEN.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        ErrorResponse errorResponse = ErrorResponse.builder()
                .code(ErrorCode.FORBIDDEN.getCode())
                .message("Direct access to internal microservice is forbidden.")
                .build();
        objectMapper.writeValue(response.getWriter(), ApiResponse.error(errorResponse));
    }
}
