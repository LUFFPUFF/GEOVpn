package com.vpn.common.security.annotations;

import com.vpn.common.security.UserRole;

import java.lang.annotation.*;

@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RequireAnyRole {

    UserRole[] value();
}
