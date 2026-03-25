package com.vpn.server.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class UserStatDto {
    private String name;
    private long value;
}
