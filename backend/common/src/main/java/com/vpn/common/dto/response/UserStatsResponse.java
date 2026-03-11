package com.vpn.common.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserStatsResponse {

    private Long telegramId;
    private Integer balance;
    private Integer totalDevices;
    private Integer activeDevices;
    private Long totalReferrals;
    private Integer totalReferralEarnings;
    private Long totalConnections;
    private Long totalTrafficBytes;
}