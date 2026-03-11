package com.vpn.user.security.aop;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.Arrays;

@Aspect
@Component
@Slf4j
public class SecurityAuditAspect {

    @Pointcut("@annotation(org.springframework.security.access.prepost.PreAuthorize) && " +
            "execution(* com.vpn.user.controller..*(..))")
    public void secureMethodsPointcut() {}

    @Before("secureMethodsPointcut()")
    public void logSecurityAccess(JoinPoint joinPoint) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        if (auth != null) {
            String user = (String) auth.getPrincipal();
            String roles = auth.getAuthorities().toString();
            String method = joinPoint.getSignature().toShortString();
            String args = Arrays.toString(joinPoint.getArgs());

            log.info("[SECURITY AUDIT] User: {} | Roles: {} | Attempting access to: {} | Args: {}",
                    user, roles, method, args);

            // todo отправить метрику в Prometheus.
        }
    }
}
