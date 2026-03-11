package com.vpn.common.security;

/**
 * Роли пользователей в системе
 */
public enum UserRole {

    /**
     * Обычный пользователь
     * Доступ к своим данным: устройства, конфигурации, баланс
     */
    USER,

    /**
     * Администратор
     * Полный доступ ко всем данным и операциям
     */
    ADMIN,

    /**
     * Микросервис (inter-service communication)
     * Доступ к внутренним API через X-Internal-Secret
     */
    SERVICE;

    /**
     * Проверить является ли роль админом
     */
    public boolean isAdmin() {
        return this == ADMIN;
    }

    /**
     * Проверить является ли роль пользователем
     */
    public boolean isUser() {
        return this == USER;
    }

    /**
     * Проверить является ли роль сервисом
     */
    public boolean isService() {
        return this == SERVICE;
    }
}
