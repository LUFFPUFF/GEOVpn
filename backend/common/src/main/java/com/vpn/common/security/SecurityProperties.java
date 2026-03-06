package com.vpn.common.security;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
@ConfigurationProperties(prefix = "service.security")
@Getter
@Setter
public class SecurityProperties {
    private String internalSecret;
    private List<Long> admins = new ArrayList<>();
}
