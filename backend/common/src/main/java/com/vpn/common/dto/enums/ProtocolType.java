package com.vpn.common.dto.enums;

import lombok.Getter;

@Getter
public enum ProtocolType {
    VLESS("VLESS with Reality"),
    HYSTERIA2("Hysteria 2"),
    TROJAN("Trojan-Go"),
    TUIC("TUIC v5"),
    SHADOWTLS("ShadowTLS v3");

    private final String description;

    ProtocolType(String description) {
        this.description = description;
    }

}
