package com.vpn.config.domain.valueobject;

import com.vpn.common.exception.ValidationException;
import lombok.Getter;
import lombok.Value;

import java.util.regex.Pattern;

/**
 * Value Object для адреса сервера
 * Гарантирует валидность IP адреса
 */
@Getter
public class ServerAddress {

    private static final Pattern IPV4_PATTERN = Pattern.compile(
            "^((25[0-5]|(2[0-4]|1\\d|[1-9]|)\\d)\\.?\\b){4}$"
    );

    private static final Pattern DOMAIN_PATTERN = Pattern.compile(
            "^[a-zA-Z0-9][a-zA-Z0-9-]{0,61}[a-zA-Z0-9](?:\\.[a-zA-Z]{2,})+$"
    );

    String value;

    public ServerAddress(String value) {
        if (value == null || value.trim().isEmpty()) {
            throw new ValidationException("Server address cannot be empty");
        }

        String trimmed = value.trim();

        if (!isValidIpv4(trimmed) && !isValidDomain(trimmed)) {
            throw new ValidationException("Invalid server address: " + value);
        }

        this.value = trimmed;
    }

    private boolean isValidIpv4(String address) {
        return IPV4_PATTERN.matcher(address).matches();
    }

    private boolean isValidDomain(String address) {
        return DOMAIN_PATTERN.matcher(address).matches();
    }

    @Override
    public String toString() {
        return value;
    }
}
