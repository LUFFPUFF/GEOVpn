package com.vpn.common.security.aspect;

import com.vpn.common.exception.ForbiddenException;
import com.vpn.common.exception.UnauthorizedException;
import com.vpn.common.security.annotations.*;
import com.vpn.common.security.UserRole;
import com.vpn.common.security.context.SecurityContext;
import com.vpn.common.security.context.SecurityContextHolder;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;

/**
 * AOP Aspect для проверки прав доступа на основе кастомных аннотаций
 *
 * Порядок проверки:
 * 1. @Public - пропускаем проверку
 * 2. @RequireUser - требует роль USER
 * 3. @RequireAdmin - требует роль ADMIN
 * 4. @RequireService - требует роль SERVICE
 * 5. @RequireAnyRole - требует любую из указанных ролей
 */
@Slf4j
@Aspect
@Component
@Order(1)
public class SecurityAspect {

    /**
     * Проверка @Public
     * Если метод помечен как публичный - пропускаем все проверки
     */
    @Before("@annotation(com.vpn.common.security.annotations.Public) || " +
            "@within(com.vpn.common.security.annotations.Public)")
    public void checkPublicAccess(JoinPoint joinPoint) {
        log.trace("Public access granted for method: {}", joinPoint.getSignature().getName());
    }


    /**
     * Проверка @RequireUser
     * Требует аутентифицированного пользователя с ролью USER
     */
    @Before("@annotation(requireUser)")
    public void checkUserAccess(JoinPoint joinPoint, RequireUser requireUser) {
        log.debug("Checking USER access for method: {}", joinPoint.getSignature().getName());

        SecurityContext context = SecurityContextHolder.getContext();

        if (context == null || context.getRole() == null) {
            log.warn("Unauthorized access attempt to USER-protected method: {}",
                    joinPoint.getSignature().getName());
            throw new UnauthorizedException("Authentication required");
        }

        if (!context.hasAnyRole(UserRole.USER, UserRole.ADMIN)) {
            log.warn("Forbidden access attempt: required=USER, actual={}", context.getRole());
            throw new ForbiddenException(UserRole.USER, context.getRole());
        }

        if (requireUser.checkOwnership()) {
            checkOwnership(joinPoint, context.getUserId());
        }

        log.trace("USER access granted: userId={}", context.getUserId());
    }

    /**
     * Проверка @RequireAdmin
     * Требует роль ADMIN
     */
    @Before("@annotation(com.vpn.common.security.annotations.RequireAdmin)")
    public void checkAdminAccess(JoinPoint joinPoint) {
        log.debug("Checking ADMIN access for method: {}", joinPoint.getSignature().getName());

        SecurityContext context = SecurityContextHolder.getContext();

        if (context == null || !context.hasRole(UserRole.ADMIN)) {
            log.warn("Forbidden access attempt to ADMIN-protected method: {}, userId={}",
                    joinPoint.getSignature().getName(),
                    context != null ? context.getUserId() : "null");
            throw new ForbiddenException("Admin role required");
        }

        log.trace("ADMIN access granted: userId={}", context.getUserId());
    }

    /**
     * Проверка @RequireService
     * Требует роль SERVICE (межсервисный вызов)
     */
    @Before("@annotation(com.vpn.common.security.annotations.RequireService)")
    public void checkServiceAccess(JoinPoint joinPoint) {
        log.debug("Checking SERVICE access for method: {}", joinPoint.getSignature().getName());

        SecurityContext context = SecurityContextHolder.getContext();

        if (context == null || !context.hasRole(UserRole.SERVICE)) {
            log.warn("Forbidden access attempt to SERVICE-protected method: {}",
                    joinPoint.getSignature().getName());
            throw new ForbiddenException("Service role required (internal API)");
        }

        log.trace("SERVICE access granted");
    }

    /**
     * Проверка @RequireAnyRole
     * Требует любую из указанных ролей
     */
    @Before("@annotation(requireAnyRole)")
    public void checkAnyRoleAccess(JoinPoint joinPoint, RequireAnyRole requireAnyRole) {
        UserRole[] allowedRoles = requireAnyRole.value();

        log.debug("Checking ANY_ROLE access for method: {}, allowed roles: {}",
                joinPoint.getSignature().getName(), allowedRoles);

        SecurityContext context = SecurityContextHolder.getContext();

        if (context == null || !context.hasAnyRole(allowedRoles)) {
            log.warn("Forbidden access attempt: required any of {}, actual={}",
                    allowedRoles, context != null ? context.getRole() : "null");
            throw new ForbiddenException(
                    String.format("One of these roles required: %s",
                            java.util.Arrays.toString(allowedRoles)));
        }

        log.trace("ANY_ROLE access granted: userId={}, role={}",
                context.getUserId(), context.getRole());
    }

    /**
     * Проверка владения ресурсом
     * Сравнивает userId из SecurityContext с userId в параметрах метода
     */
    private void checkOwnership(JoinPoint joinPoint, Long currentUserId) {
        Object[] args = joinPoint.getArgs();

        for (Object arg : args) {
            if (arg == null) continue;

            try {
                Method getUserIdMethod = arg.getClass().getMethod("getUserId");
                Long resourceUserId = (Long) getUserIdMethod.invoke(arg);

                if (resourceUserId != null && !resourceUserId.equals(currentUserId)) {
                    log.warn("Ownership check failed: currentUserId={}, resourceUserId={}",
                            currentUserId, resourceUserId);
                    throw new ForbiddenException("You don't own this resource");
                }

                log.trace("Ownership check passed: userId={}", currentUserId);
                return;

            } catch (NoSuchMethodException ignored) {
            } catch (ForbiddenException e) {
                throw e;
            } catch (Exception e) {
                log.warn("Reflection error while checking ownership on arg: {}", arg.getClass().getSimpleName(), e);
            }
        }
    }


}
