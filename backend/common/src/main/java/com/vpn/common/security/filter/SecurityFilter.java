package com.vpn.common.security.filter;

import com.vpn.common.security.UserRole;
import com.vpn.common.security.context.SecurityContext;
import com.vpn.common.security.context.SecurityContextHolder;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Security Filter для создания SecurityContext из HTTP headers
 *
 * Порядок проверки:
 * 1. Проверка X-Internal-Secret → роль SERVICE
 * 2. Проверка X-Admin-Token → роль ADMIN
 * 3. Проверка X-User-Id → роль USER
 *
 * Заполняет SecurityContextHolder для использования в AOP Aspect
 */
@Slf4j
@Component
@Order(1)
@RequiredArgsConstructor
public class SecurityFilter extends OncePerRequestFilter {

    @Value("${service.security.internal-secret}")
    private String internalSecret;

    @Value("${service.security.admin-token}")
    private String adminToken;

    @Value("${service.security.admins}")
    private String adminUserIds;

    private static final String HEADER_INTERNAL_SECRET = "X-Internal-Secret";
    private static final String HEADER_ADMIN_TOKEN = "X-Admin-Token";
    private static final String HEADER_USER_ID = "X-User-Id";
    private static final String HEADER_REQUEST_ID = "X-Request-ID";

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException
    {

        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            SecurityContext context = createSecurityContext(request);

            if (context != null) {
                SecurityContextHolder.setContext(context);
                log.debug("SecurityContext created: userId={}, role={}, ip={}",
                        context.getUserId(),
                        context.getRole(),
                        context.getIpAddress());
            }

            filterChain.doFilter(request, response);
        } finally {
            SecurityContextHolder.clear();
        }
    }

    private SecurityContext createSecurityContext(HttpServletRequest request) {
        String requestId = getOrGenerateRequestId(request);
        String ipAddress = getClientIpAddress(request);
        String userAgent = request.getHeader("User-Agent");

        SecurityContext.SecurityContextBuilder builder = SecurityContext.builder()
                .requestId(requestId)
                .ipAddress(ipAddress)
                .userAgent(userAgent);

        Set<UserRole> roles = new HashSet<>();
        boolean isInternal = false;

        String internalSecretHeader = request.getHeader(HEADER_INTERNAL_SECRET);
        if (internalSecretHeader != null && internalSecretHeader.equals(internalSecret)) {
            roles.add(UserRole.SERVICE);
            isInternal = true;
        }

        String adminTokenHeader = request.getHeader(HEADER_ADMIN_TOKEN);
        if (adminTokenHeader != null && adminTokenHeader.equals(adminToken)) {
            roles.add(UserRole.ADMIN);
        }

        String userIdHeader = request.getHeader(HEADER_USER_ID);
        if (userIdHeader != null) {
            try {
                Long userId = Long.parseLong(userIdHeader);
                builder.userId(userId);
                roles.add(UserRole.USER);

                if (isAdminUser(userId)) {
                    roles.add(UserRole.ADMIN);
                }
            } catch (NumberFormatException e) {
                log.warn("Invalid X-User-Id header: {}", userIdHeader);
            }
        }

        if (roles.isEmpty()) {
            return null;
        }

        UserRole primaryRole;
        if (roles.contains(UserRole.ADMIN)) {
            primaryRole = UserRole.ADMIN;
        } else if (roles.contains(UserRole.USER)) {
            primaryRole = UserRole.USER;
        } else {
            primaryRole = UserRole.SERVICE;
        }

        return builder
                .role(primaryRole)
                .roles(roles)
                .internal(isInternal)
                .build();
    }

    /**
     * Получить или сгенерировать Request ID для трейсинга
     */
    private String getOrGenerateRequestId(HttpServletRequest request) {
        String requestId = request.getHeader(HEADER_REQUEST_ID);

        if (requestId == null || requestId.isEmpty()) {
            requestId = UUID.randomUUID().toString();
        }

        return requestId;
    }

    /**
     * Получить реальный IP адрес клиента (с учетом прокси)
     */
    private String getClientIpAddress(HttpServletRequest request) {
        String[] headerNames = {
                "X-Forwarded-For",
                "Proxy-Client-IP",
                "WL-Proxy-Client-IP",
                "HTTP_X_FORWARDED_FOR",
                "HTTP_X_FORWARDED",
                "HTTP_X_CLUSTER_CLIENT_IP",
                "HTTP_CLIENT_IP",
                "HTTP_FORWARDED_FOR",
                "HTTP_FORWARDED",
                "HTTP_VIA",
                "REMOTE_ADDR"
        };

        for (String header : headerNames) {
            String ip = request.getHeader(header);
            if (ip != null && !ip.isEmpty() && !"unknown".equalsIgnoreCase(ip)) {
                if (ip.contains(",")) {
                    ip = ip.split(",")[0].trim();
                }
                return ip;
            }
        }

        return request.getRemoteAddr();
    }

    /**
     * Проверить является ли userId администратором
     * Проверяется по списку из конфигурации
     */
    private boolean isAdminUser(Long userId) {
        if (adminUserIds == null || adminUserIds.isEmpty()) {
            return false;
        }

        String[] adminIds = adminUserIds.split(",");
        for (String adminId : adminIds) {
            try {
                if (Long.parseLong(adminId.trim()) == userId) {
                    return true;
                }
            } catch (NumberFormatException e) {
                log.warn("Invalid admin user ID in config: {}", adminId);
            }
        }

        return false;
    }
}
