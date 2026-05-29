package com.vpn.user.service;

import com.vpn.common.dto.ApiResponse;
import com.vpn.common.dto.enums.DeviceType;
import com.vpn.common.dto.response.DeviceLimitStatus;
import com.vpn.common.util.ValidationUtils;
import com.vpn.user.domain.entity.Device;
import com.vpn.user.dto.mapper.DeviceMapper;
import com.vpn.common.dto.request.DeviceCreateRequest;
import com.vpn.common.dto.response.DeviceResponse;
import com.vpn.user.exception.DeviceNotFoundException;
import com.vpn.user.exception.MaxDevicesExceededException;
import com.vpn.user.exception.UserNotFoundException;
import com.vpn.user.grpc.VpnServiceClient;
import com.vpn.user.repository.DeviceRepository;
import com.vpn.user.repository.UserRepository;
import com.vpn.user.service.interf.DeviceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DeviceServiceImpl implements DeviceService {

    private final DeviceRepository deviceRepository;
    private final UserRepository userRepository;
    private final DeviceMapper deviceMapper;
    private final VpnServiceClient vpnServiceClient;

    @org.springframework.context.annotation.Lazy
    @org.springframework.beans.factory.annotation.Autowired
    private DeviceService self;

    @Override
    @Transactional
    @CacheEvict(value = "user-devices", key = "#p0.userId")
    public DeviceResponse createDevice(DeviceCreateRequest request) {
        log.info("Creating device: userId={}, deviceName={}, deviceType={}",
                request.getUserId(), request.getDeviceName(), request.getDeviceType());

        ValidationUtils.validateNotNull(request.getUserId(), "User ID");
        ValidationUtils.validateNotEmpty(request.getDeviceName(), "Device name");
        ValidationUtils.validateNotNull(request.getDeviceType(), "Device type");

        if (!userRepository.existsByTelegramId(request.getUserId())) {
            throw new UserNotFoundException(request.getUserId());
        }

        long activeDevicesCount = deviceRepository.countActiveDevicesByUserId(request.getUserId());

        int maxAllowed = self.getMaxDevicesForUser(request.getUserId());

        if (activeDevicesCount >= maxAllowed) {
            log.warn("Max devices limit exceeded: userId={}, current={}, max={}",
                    request.getUserId(), activeDevicesCount, maxAllowed);
            throw new MaxDevicesExceededException(request.getUserId());
        }

        Device device = Device.builder()
                .userId(request.getUserId())
                .deviceName(request.getDeviceName())
                .deviceType(request.getDeviceType())
                .uuid(UUID.randomUUID())
                .isActive(true)
                .build();

        Device savedDevice = deviceRepository.save(device);

        log.info("Device created: userId={}, deviceId={}, uuid={}",
                request.getUserId(), savedDevice.getId(), savedDevice.getUuid());

        return deviceMapper.toResponse(savedDevice);
    }

    @Override
    @Cacheable(value = "user-max-devices", key = "#userId")
    public int getMaxDevicesForUser(Long userId) {
        try {
            log.info("Fetching device limit from config-service for user {}", userId);
            ApiResponse<DeviceLimitStatus> response = vpnServiceClient.getDeviceLimit(userId);
            if (response != null && response.getData() != null) {
                return response.getData().getMaxDevices();
            }
        } catch (Exception e) {
            log.warn("Failed to fetch device limit for userId={}, using default=1", userId, e);
        }
        return 1;
    }

    @Override
    @Cacheable(value = "devices", key = "#p0.toString()")
    public DeviceResponse getDeviceByUuid(UUID uuid) {
        log.debug("Fetching device by UUID: {}", uuid);
        ValidationUtils.validateNotNull(uuid, "Device UUID");
        Device device = deviceRepository.findByUuid(uuid)
                .orElseThrow(() -> new DeviceNotFoundException(uuid));
        return deviceMapper.toResponse(device);
    }

    @Override
    @Cacheable(value = "user-devices", key = "#telegramId")
    public List<DeviceResponse> getUserActiveDevices(Long telegramId) {
        log.debug("Fetching active devices for user: {}", telegramId);
        ValidationUtils.validateTelegramId(telegramId);
        return deviceRepository.findByUserIdAndIsActiveTrue(telegramId)
                .stream()
                .map(deviceMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    @org.springframework.cache.annotation.Caching(evict = {
            @CacheEvict(value = "devices", key = "#uuid.toString()"),
            @CacheEvict(value = "user-devices", key = "#telegramId")
    })
    public void deactivateDevice(UUID uuid, Long telegramId) {
        log.info("Deactivating device: uuid={}, requestedBy={}", uuid, telegramId);

        ValidationUtils.validateNotNull(uuid, "Device UUID");
        ValidationUtils.validateTelegramId(telegramId);

        Device device = deviceRepository.findByUuid(uuid)
                .orElseThrow(() -> new DeviceNotFoundException(uuid));

        if (!device.getUserId().equals(telegramId)) {
            log.warn("Unauthorized device deactivation attempt: uuid={}, userId={}, requestedBy={}",
                    uuid, device.getUserId(), telegramId);
            throw new DeviceNotFoundException(uuid);
        }

        device.deactivate();
        deviceRepository.save(device);

        log.info("Device deactivated: uuid={}, userId={}", uuid, telegramId);
    }

    @Override
    @Transactional
    @org.springframework.cache.annotation.Caching(evict = {
            @CacheEvict(value = "devices", key = "#uuid.toString()"),
            @CacheEvict(value = "user-devices", key = "#telegramId")
    })
    public void deleteDevice(UUID uuid, Long telegramId) {
        log.info("Hard deleting device: uuid={}, requestedBy={}", uuid, telegramId);

        ValidationUtils.validateNotNull(uuid, "Device UUID");
        ValidationUtils.validateTelegramId(telegramId);

        Device device = deviceRepository.findByUuid(uuid)
                .orElseThrow(() -> new DeviceNotFoundException(uuid));

        if (!device.getUserId().equals(telegramId)) {
            log.warn("Unauthorized device delete attempt: uuid={}, ownerId={}, requestedBy={}",
                    uuid, device.getUserId(), telegramId);
            throw new DeviceNotFoundException(uuid);
        }

        deviceRepository.delete(device);
        log.info("Device hard deleted: uuid={}, userId={}", uuid, telegramId);
    }

    @Override
    @Transactional
    @org.springframework.cache.annotation.Caching(evict = {
            @CacheEvict(value = "user-devices", key = "#userId"),
            @CacheEvict(value = "devices", allEntries = true)
    })
    public void deleteDeviceById(Long deviceId, Long userId) {
        log.info("Internal hard delete: deviceId={}, userId={}", deviceId, userId);

        deviceRepository.findById(deviceId)
                .filter(d -> d.getUserId().equals(userId))
                .ifPresentOrElse(
                        device -> {
                            deviceRepository.delete(device);
                            log.info("Device hard deleted by ID: deviceId={}, userId={}", deviceId, userId);
                        },
                        () -> log.warn("Device not found or owner mismatch on internal delete: deviceId={}, userId={}",
                                deviceId, userId)
                );
    }

    @Override
    @Transactional
    @CacheEvict(value = "devices", key = "#uuid.toString()")
    public void updateLastConnected(UUID uuid) {
        log.trace("Updating last connected time: uuid={}", uuid);

        deviceRepository.findByUuid(uuid)
                .ifPresent(device -> {
                    device.updateLastConnected();
                    deviceRepository.save(device);
                });
    }

    @Override
    public boolean isDeviceOwnedByUser(UUID deviceUuid, Long telegramId) {
        ValidationUtils.validateNotNull(deviceUuid, "Device UUID");
        ValidationUtils.validateTelegramId(telegramId);

        return deviceRepository.findByUuid(deviceUuid)
                .map(device -> device.getUserId().equals(telegramId))
                .orElse(false);
    }

    @Deprecated
    @Transactional
    public DeviceResponse syncDeviceWithPlatform(Long userId, String platform) {
        DeviceType type = mapPlatformToDeviceType(platform);

        List<Device> existingDevices = deviceRepository.findByUserIdAndDeviceTypeAndIsActiveTrue(userId, type);

        if (!existingDevices.isEmpty()) {
            return deviceMapper.toResponse(existingDevices.getFirst());
        }

        DeviceCreateRequest request = DeviceCreateRequest.builder()
                .userId(userId)
                .deviceName(type.name().charAt(0) + type.name().substring(1).toLowerCase() + " Device")
                .deviceType(type)
                .build();

        return createDevice(request);
    }

    @Deprecated
    private DeviceType mapPlatformToDeviceType(String platform) {
        if (platform == null) return DeviceType.UNKNOWN;
        return switch (platform.toLowerCase()) {
            case "ios"                  -> DeviceType.IOS;
            case "android"              -> DeviceType.ANDROID;
            case "macos"                -> DeviceType.MACOS;
            case "tdesktop", "windows"  -> DeviceType.WINDOWS;
            case "linux"                -> DeviceType.LINUX;
            case "desktop"              -> DeviceType.DESKTOP;
            default                     -> DeviceType.UNKNOWN;
        };
    }
}