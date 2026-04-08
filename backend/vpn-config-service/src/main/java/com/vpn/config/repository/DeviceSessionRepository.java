package com.vpn.config.repository;

import com.vpn.config.domain.entity.DeviceSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface DeviceSessionRepository extends JpaRepository<DeviceSession, Long> {

    Optional<DeviceSession> findByUserIdAndDeviceFingerprint(Long userId, String fingerprint);

    List<DeviceSession> findByUserIdAndIsActiveTrue(Long userId);

    int countByUserIdAndIsActiveTrue(Long userId);

    @Modifying
    @Query("UPDATE DeviceSession s SET s.isActive = false WHERE s.userId = :userId AND s.deviceFingerprint = :fp")
    void deactivateSession(@Param("userId") Long userId, @Param("fp") String fingerprint);

    @Modifying
    @Query("UPDATE DeviceSession s SET s.isActive = false WHERE s.userId = :userId")
    void deactivateAllSessions(@Param("userId") Long userId);

    boolean existsByUserIdAndDeviceFingerprintAndIsActiveTrue(Long userId, String fingerprint);
}
