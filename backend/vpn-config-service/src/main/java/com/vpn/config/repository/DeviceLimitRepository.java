package com.vpn.config.repository;

import com.vpn.config.domain.entity.DeviceLimit;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface DeviceLimitRepository extends JpaRepository<DeviceLimit, Long> {

    Optional<DeviceLimit> findByUserId(Long userId);
}
