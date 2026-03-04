package com.vpn.common.util;

import java.util.UUID;

public final class StringUtils {
    private StringUtils() {}

    public static boolean isNullOrEmpty(String str) {
        return str == null || str.trim().isEmpty();
    }

    public static String generateUuid() {
        return UUID.randomUUID().toString();
    }
}
