package com.vpn.user.service;

import com.vpn.common.constant.AppConstants;
import com.vpn.user.domain.entity.Device;
import com.vpn.user.domain.entity.DeviceType;
import com.vpn.user.dto.mapper.DeviceMapper;
import com.vpn.user.dto.request.DeviceCreateRequest;
import com.vpn.user.dto.response.DeviceResponse;
import com.vpn.user.exception.DeviceNotFoundException;
import com.vpn.user.exception.MaxDevicesExceededException;
import com.vpn.user.repository.DeviceRepository;
import com.vpn.user.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DeviceServiceImplTest {

    @Mock
    private DeviceRepository deviceRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private DeviceMapper deviceMapper;

    @InjectMocks
    private DeviceServiceImpl deviceService;

    @Test
    @DisplayName("Создание устройства должно пройти успешно, если лимит не достигнут.")
    void shouldCreateDeviceSuccessfully() {
        DeviceCreateRequest request = new DeviceCreateRequest();
        request.setUserId(123L);
        request.setDeviceName("My iPhone");
        request.setDeviceType(DeviceType.IOS);

        when(userRepository.existsByTelegramId(123L)).thenReturn(true);
        when(deviceRepository.countActiveDevicesByUserId(123L)).thenReturn(2L);

        Device savedDevice = Device.builder().id(1L).uuid(UUID.randomUUID()).build();
        when(deviceRepository.save(any(Device.class))).thenReturn(savedDevice);

        DeviceResponse expectedResponse = new DeviceResponse();
        expectedResponse.setUuid(savedDevice.getUuid());
        when(deviceMapper.toResponse(savedDevice)).thenReturn(expectedResponse);

        DeviceResponse actualResponse = deviceService.createDevice(request);

        assertThat(actualResponse.getUuid()).isEqualTo(savedDevice.getUuid());
        verify(deviceRepository).save(any(Device.class));
    }

    @Test
    @DisplayName("При достижении лимита следует выбрасывать исключение MaxDevicesExceededException.")
    void shouldThrowMaxDevicesException() {
        DeviceCreateRequest request = new DeviceCreateRequest();
        request.setUserId(123L);
        request.setDeviceName("My iPhone");
        request.setDeviceType(DeviceType.IOS);

        when(userRepository.existsByTelegramId(123L)).thenReturn(true);
        when(deviceRepository.countActiveDevicesByUserId(123L)).thenReturn((long) AppConstants.MAX_DEVICES_PER_USER);

        assertThatThrownBy(() -> deviceService.createDevice(request))
                .isInstanceOf(MaxDevicesExceededException.class);

        verify(deviceRepository, never()).save(any());
    }

    @Test
    @DisplayName("Устройство должно быть деактивировано, если оно принадлежит пользователю.")
    void shouldDeactivateDeviceSuccessfully() {
        UUID deviceUuid = UUID.randomUUID();
        Long userId = 123L;

        Device device = Device.builder()
                .uuid(deviceUuid)
                .userId(userId)
                .isActive(true)
                .build();

        when(deviceRepository.findByUuid(deviceUuid)).thenReturn(Optional.of(device));

        deviceService.deactivateDevice(deviceUuid, userId);

        assertThat(device.getIsActive()).isFalse();
        verify(deviceRepository).save(device);
    }

    @Test
    @DisplayName("В случае, если пользователь НЕ является владельцем устройства, должно быть выброшено исключение DeviceNotFoundException (Security).")
    void shouldThrowExceptionWhenDeactivatingForeignDevice() {
        UUID deviceUuid = UUID.randomUUID();
        Long ownerId = 123L;
        Long hackerId = 666L;

        Device device = Device.builder()
                .uuid(deviceUuid)
                .userId(ownerId)
                .isActive(true)
                .build();

        when(deviceRepository.findByUuid(deviceUuid)).thenReturn(Optional.of(device));

        assertThatThrownBy(() -> deviceService.deactivateDevice(deviceUuid, hackerId))
                .isInstanceOf(DeviceNotFoundException.class);

        assertThat(device.getIsActive()).isTrue();
        verify(deviceRepository, never()).save(any());
    }
}