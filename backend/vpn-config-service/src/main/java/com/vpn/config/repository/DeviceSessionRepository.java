package com.vpn.config.repository;

import com.vpn.config.domain.entity.DeviceSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface DeviceSessionRepository extends JpaRepository<DeviceSession, Long> {

    Optional<DeviceSession> findByUserIdAndDeviceFingerprint(Long userId, String fingerprint);

    List<DeviceSession> findByUserIdAndIsActiveTrue(Long userId);

    int countByUserIdAndIsActiveTrue(Long userId);

    Optional<DeviceSession> findByUserIdAndVlessUuid(Long userId, UUID vlessUuid);

    @Modifying
    @Query("UPDATE DeviceSession s SET s.isActive = false WHERE s.userId = :userId AND s.deviceFingerprint = :fp")
    void deactivateSession(@Param("userId") Long userId, @Param("fp") String fingerprint);

    @Modifying
    @Query("UPDATE DeviceSession s SET s.isActive = false WHERE s.userId = :userId")
    void deactivateAllSessions(@Param("userId") Long userId);

    boolean existsByUserIdAndDeviceFingerprintAndIsActiveTrue(Long userId, String fingerprint);

    @Modifying
    @Transactional
    @Query(value = """
        INSERT INTO device_sessions (
            device_fingerprint, device_name, is_active, last_ip, 
            last_seen_at, user_agent, user_id, vless_uuid
        ) VALUES (
            :fingerprint, :name, :isActive, :lastIp, 
            :lastSeenAt, :userAgent, :userId, :vlessUuid
        )
        ON CONFLICT (user_id, vless_uuid) 
        DO UPDATE SET 
            device_fingerprint = EXCLUDED.device_fingerprint,
            device_name = EXCLUDED.device_name,
            last_ip = EXCLUDED.last_ip,
            last_seen_at = EXCLUDED.last_seen_at,
            user_agent = EXCLUDED.user_agent,
            is_active = EXCLUDED.is_active
        """, nativeQuery = true)
    void upsertSession(
            @Param("fingerprint") String fingerprint,
            @Param("name") String name,
            @Param("isActive") boolean isActive,
            @Param("lastIp") String lastIp,
            @Param("lastSeenAt") LocalDateTime lastSeenAt,
            @Param("userAgent") String userAgent,
            @Param("userId") Long userId,
            @Param("vlessUuid") UUID vlessUuid
    );
}
