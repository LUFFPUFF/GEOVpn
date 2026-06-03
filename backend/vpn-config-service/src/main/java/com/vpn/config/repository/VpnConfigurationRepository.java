package com.vpn.config.repository;

import com.vpn.config.domain.entity.VpnConfiguration;
import com.vpn.common.dto.enums.ConfigStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface VpnConfigurationRepository extends JpaRepository<VpnConfiguration, Long> {

    Optional<VpnConfiguration> findByDeviceIdAndStatus(Long deviceId, ConfigStatus status);

    Optional<VpnConfiguration> findByVlessUuid(UUID vlessUuid);

    List<VpnConfiguration> findByUserIdAndStatus(Long userId, ConfigStatus status);

    Optional<VpnConfiguration> findByDeviceId(Long deviceId);

    List<VpnConfiguration> findByStatus(ConfigStatus status);

    @Modifying
    @Transactional
    @Query("DELETE FROM VpnConfiguration")
    int deleteAllConfigsFast();

    @Query("SELECT DISTINCT v.userId FROM VpnConfiguration v")
    List<Long> findAllDistinctUserIds();
}
