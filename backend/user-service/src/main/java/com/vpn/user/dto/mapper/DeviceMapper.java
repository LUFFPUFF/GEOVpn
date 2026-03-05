package com.vpn.user.dto.mapper;

import com.vpn.user.domain.entity.Device;
import com.vpn.user.dto.response.DeviceResponse;
import org.springframework.stereotype.Component;

@Component
public class DeviceMapper {

    public DeviceResponse toResponse(Device device) {
        return DeviceResponse.builder()
                .id(device.getId())
                .deviceName(device.getDeviceName())
                .deviceType(device.getDeviceType())
                .uuid(device.getUuid())
                .isActive(device.getIsActive())
                .lastConnectedAt(device.getLastConnectedAt())
                .createdAt(device.getCreatedAt())
                .build();
    }
}
