package com.vpn.common.constant;

import lombok.Getter;

@Getter
public enum Role {
    USER("ROLE_USER"),
    ADMIN("ROLE_ADMIN"),
    SERVICE("ROLE_SERVICE");

    private final String value;

    Role(String value) {
        this.value = value;
    }

}
