package com.vpn.common.security.context;

import com.vpn.common.security.UserRole;
import lombok.Builder;
import lombok.Data;

import java.util.Set;

/**
 * Контекст безопасности для текущего запроса
 * Хранится в ThreadLocal через SecurityContextHolder
 */

@Data
@Builder
public class SecurityContext {

    /**
     * Telegram ID пользователя (для роли USER)
     * null для ADMIN и SERVICE
     */
    private Long userId;

    /**
     * Роль пользователя
     */
    private UserRole role;

    /**
     * Список ролей (если пользователь имеет несколько ролей)
     */
    private Set<UserRole> roles;

    /**
     * IP адрес клиента
     */
    private String ipAddress;

    /**
     * User-Agent
     */
    private String userAgent;

    /**
     * Request ID для трейсинга
     */
    private String requestId;

    /**
     * Является ли запрос внутренним (от другого микросервиса)
     */
    private boolean internal;

    /**
     * Проверить является ли пользователь админом
     */
    public boolean isAdmin() {
        return role == UserRole.ADMIN ||
                (roles != null && roles.contains(UserRole.ADMIN));
    }

    /**
     * Проверить является ли пользователь обычным юзером
     */
    public boolean isUser() {
        return role == UserRole.USER ||
                (roles != null && roles.contains(UserRole.USER));
    }

    /**
     * Проверить является ли запрос от микросервиса
     */
    public boolean isService() {
        return role == UserRole.SERVICE ||
                (roles != null && roles.contains(UserRole.SERVICE));
    }

    /**
     * Проверить имеет ли пользователь указанную роль
     */
    public boolean hasRole(UserRole requiredRole) {
        if (role == requiredRole) {
            return true;
        }
        return roles != null && roles.contains(requiredRole);
    }

    /**
     * Проверить имеет ли пользователь хотя бы одну из указанных ролей
     */
    public boolean hasAnyRole(UserRole... requiredRoles) {
        for (UserRole requiredRole : requiredRoles) {
            if (hasRole(requiredRole)) {
                return true;
            }
        }
        return false;
    }
}
