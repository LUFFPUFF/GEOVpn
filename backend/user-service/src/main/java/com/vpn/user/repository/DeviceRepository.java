package com.vpn.user.repository;

import com.vpn.common.dto.enums.DeviceType;
import com.vpn.user.domain.entity.Device;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface DeviceRepository extends JpaRepository<Device, Long> {

    List<Device> findByUserIdAndIsActiveTrue(Long userId);

    Optional<Device> findByUuid(UUID uuid);

    Optional<Device> findByUserIdAndDeviceTypeAndIsActiveTrue(Long userId, DeviceType deviceType);

    @Query("SELECT COUNT(d) FROM Device d WHERE d.userId = :userId AND d.isActive = true")
    long countActiveDevicesByUserId(@Param("userId") Long userId);

    @Query("SELECT COUNT(DISTINCT d.userId) FROM Device d WHERE d.isActive = true")
    long countDistinctUserIdByIsActiveTrue();

    boolean existsByUuid(UUID uuid);

    long countByUserId(Long userId);
}
