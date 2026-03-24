package com.vpn.common.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminDashboardResponse {
    private long totalUsers;
    private long activeSubscriptions;
    private int totalServers;
    private int activeServers;
    private long totalTrafficGb;
    private int totalBalanceRub;
}
