package com.vpn.common.security.context;

import com.vpn.common.security.UserRole;
import lombok.extern.slf4j.Slf4j;

/**
 * Holder для SecurityContext на уровне потока (ThreadLocal)
 *
 * Используется для получения информации о текущем пользователе в любом месте кода
 *
 * Пример:
 * <pre>
 * {@code
 * Long userId = SecurityContextHolder.getUserId();
 * UserRole role = SecurityContextHolder.getRole();
 * boolean isAdmin = SecurityContextHolder.isAdmin();
 * }
 * </pre>
 */

@Slf4j
public class SecurityContextHolder {

    private static final ThreadLocal<SecurityContext> contextHolder = new ThreadLocal<>();

    /**
     * Установить SecurityContext для текущего потока
     */
    public static void setContext(SecurityContext context) {
        if (context != null) {
            log.trace("Setting SecurityContext: userId={}, role={}",
                    context.getUserId(), context.getRole());
        }
        contextHolder.set(context);
    }

    /**
     * Получить SecurityContext для текущего потока
     */
    public static SecurityContext getContext() {
        return contextHolder.get();
    }

    /**
     * Очистить SecurityContext (ВАЖНО вызывать после обработки запроса!)
     */
    public static void clear() {
        log.trace("Clearing SecurityContext");
        contextHolder.remove();
    }

    /**
     * Получить ID текущего пользователя (Telegram ID)
     * @return userId или null если не аутентифицирован
     */
    public static Long getUserId() {
        SecurityContext context = getContext();
        return context != null ? context.getUserId() : null;
    }

    /**
     * Получить роль текущего пользователя
     * @return роль или null
     */
    public static UserRole getRole() {
        SecurityContext context = getContext();
        return context != null ? context.getRole() : null;
    }

    /**
     * Проверить является ли текущий пользователь админом
     */
    public static boolean isAdmin() {
        SecurityContext context = getContext();
        return context != null && context.isAdmin();
    }

    /**
     * Проверить является ли текущий пользователь обычным юзером
     */
    public static boolean isUser() {
        SecurityContext context = getContext();
        return context != null && context.isUser();
    }

    /**
     * Проверить является ли запрос от микросервиса
     */
    public static boolean isService() {
        SecurityContext context = getContext();
        return context != null && context.isService();
    }

    /**
     * Проверить аутентифицирован ли пользователь
     */
    public static boolean isAuthenticated() {
        SecurityContext context = getContext();
        return context != null && context.getRole() != null;
    }

    /**
     * Получить IP адрес клиента
     */
    public static String getIpAddress() {
        SecurityContext context = getContext();
        return context != null ? context.getIpAddress() : null;
    }

    /**
     * Получить Request ID
     */
    public static String getRequestId() {
        SecurityContext context = getContext();
        return context != null ? context.getRequestId() : null;
    }

    /**
     * Проверить имеет ли пользователь указанную роль
     */
    public static boolean hasRole(UserRole role) {
        SecurityContext context = getContext();
        return context != null && context.hasRole(role);
    }

    /**
     * Проверить имеет ли пользователь хотя бы одну из указанных ролей
     */
    public static boolean hasAnyRole(UserRole... roles) {
        SecurityContext context = getContext();
        return context != null && context.hasAnyRole(roles);
    }
}
