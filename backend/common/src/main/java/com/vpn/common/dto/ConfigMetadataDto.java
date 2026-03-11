package com.vpn.common.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConfigMetadataDto implements Serializable {
    private Long configId;
    private Long userId;
    private Long deviceId;
}

