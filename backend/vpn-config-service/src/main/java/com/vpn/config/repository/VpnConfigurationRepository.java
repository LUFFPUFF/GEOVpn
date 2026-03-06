package com.vpn.config.repository;

import com.vpn.config.domain.entity.VpnConfiguration;
import com.vpn.config.domain.enums.ConfigStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface VpnConfigurationRepository extends JpaRepository<VpnConfiguration, Long> {

    Optional<VpnConfiguration> findByDeviceIdAndStatus(Long deviceId, ConfigStatus status);

    Optional<VpnConfiguration> findByVlessUuid(UUID vlessUuid);

    List<VpnConfiguration> findByUserIdAndStatus(Long userId, ConfigStatus status);

    boolean existsByDeviceIdAndStatus(Long deviceId, ConfigStatus status);

    @Query("SELECT COUNT(c) FROM VpnConfiguration c WHERE c.userId = :userId AND c.status = 'ACTIVE'")
    long countActiveConfigsByUserId(@Param("userId") Long userId);

    List<VpnConfiguration> findByServerIdAndStatus(Integer serverId, ConfigStatus status);
}
