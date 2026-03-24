package com.vpn.common.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TrafficSummaryDto {
    private Long bytesIn;
    private Long bytesOut;
    private Long costKopecks;
}