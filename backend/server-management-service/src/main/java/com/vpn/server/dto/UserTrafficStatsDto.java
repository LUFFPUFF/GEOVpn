package com.vpn.server.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class UserTrafficStatsDto {
    private Long userId;
    private Long totalUp;
    private Long totalDown;
}
