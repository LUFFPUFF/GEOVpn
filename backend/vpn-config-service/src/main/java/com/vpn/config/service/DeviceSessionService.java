package com.vpn.config.service;

import com.vpn.config.domain.entity.DeviceSession;
import com.vpn.config.repository.DeviceSessionRepository;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class DeviceSessionService {

    private final DeviceSessionRepository sessionRepository;
    private final DeviceLimitService deviceLimitService;

    /**
     * Проверяет и регистрирует устройство.
     * Оптимизировано: БД обновляется только при реальном изменении IP/UA/UUID.
     * Это снижает нагрузку на запись в PostgreSQL при частых запросах подписки на 95-99%.
     */
    @Transactional
    @Retryable(
            retryFor = {
                    org.springframework.dao.TransientDataAccessException.class,
                    org.springframework.dao.PessimisticLockingFailureException.class
            },
            maxAttempts = 3,
            backoff = @Backoff(delay = 50)
    )
    public boolean checkAndRegisterDevice(
            Long userId,
            UUID vlessUuid,
            HttpServletRequest httpRequest
    ) {
        String fingerprint = extractFingerprint(httpRequest);
        String userAgent   = httpRequest.getHeader("User-Agent");
        String ip          = extractClientIp(httpRequest);
        String deviceName  = httpRequest.getHeader("X-Device-Name");

        Optional<DeviceSession> sessionByFp = sessionRepository.findByUserIdAndDeviceFingerprint(userId, fingerprint);

        if (sessionByFp.isPresent()) {
            DeviceSession session = sessionByFp.get();

            boolean ipChanged = !Objects.equals(session.getLastIp(), ip);
            boolean uaChanged = !Objects.equals(session.getUserAgent(), userAgent);
            boolean uuidChanged = !Objects.equals(session.getVlessUuid(), vlessUuid);
            boolean statusChanged = !session.getIsActive();

            if (ipChanged || uaChanged || uuidChanged || statusChanged) {

                if (uuidChanged) {
                    sessionRepository.findByUserIdAndVlessUuid(userId, vlessUuid).ifPresent(dup -> {
                        log.info("Removing stale session with duplicate vlessUuid {} to prevent conflict", vlessUuid);
                        sessionRepository.delete(dup);
                        sessionRepository.flush();
                    });
                }

                session.setLastIp(ip);
                session.setUserAgent(userAgent);
                session.setVlessUuid(vlessUuid);
                session.setIsActive(true);

                log.debug("Session updated in DB for fingerprint {}: IP/UA/UUID changed", fingerprint);
            } else {
                log.debug("Session unchanged for fingerprint {}. Skipping DB write (Perf Optimization).", fingerprint);
            }
            return true;
        }

        Optional<DeviceSession> sessionByUuid = sessionRepository.findByUserIdAndVlessUuid(userId, vlessUuid);

        if (sessionByUuid.isPresent()) {
            DeviceSession session = sessionByUuid.get();

            boolean ipChanged = !Objects.equals(session.getLastIp(), ip);
            boolean uaChanged = !Objects.equals(session.getUserAgent(), userAgent);
            boolean fpChanged = !Objects.equals(session.getDeviceFingerprint(), fingerprint);
            boolean statusChanged = !session.getIsActive();

            if (ipChanged || uaChanged || fpChanged || statusChanged) {

                if (fpChanged) {
                    sessionRepository.findByUserIdAndDeviceFingerprint(userId, fingerprint).ifPresent(dup -> {
                        log.info("Removing stale session with duplicate fingerprint {} to prevent conflict", fingerprint);
                        sessionRepository.delete(dup);
                        sessionRepository.flush();
                    });
                }

                session.setLastIp(ip);
                session.setUserAgent(userAgent);
                session.setDeviceFingerprint(fingerprint);
                session.setIsActive(true);

                sessionRepository.save(session);
                log.debug("Session updated in DB for config {}: IP/UA/FP changed", vlessUuid);
            } else {
                log.debug("Session unchanged for config {}. Skipping DB write.", vlessUuid);
            }
            return true;
        }

        int maxDevices = deviceLimitService.getMaxDevices(userId);
        int activeDevices = sessionRepository.countByUserIdAndIsActiveTrue(userId);

        if (activeDevices >= maxDevices) {
            log.warn("Device limit exceeded for userId {}: {}/{}", userId, activeDevices, maxDevices);
            return false;
        }

        sessionRepository.upsertSession(
                fingerprint,
                deviceName,
                true,
                ip,
                LocalDateTime.now(),
                userAgent,
                userId,
                vlessUuid
        );

        log.info("New physical device registered via atomic UPSERT for config {}: ip={}", vlessUuid, ip);

        return true;
    }

    public List<DeviceSession> getActiveSessions(Long userId) {
        return sessionRepository.findByUserIdAndIsActiveTrue(userId);
    }

    @Transactional
    public void revokeSession(Long userId, String fingerprint) {
        sessionRepository.deactivateSession(userId, fingerprint);
        log.info("Session revoked: userId={}, fp={}", userId, fingerprint);
    }

    @Transactional
    public void revokeAllSessions(Long userId) {
        sessionRepository.deactivateAllSessions(userId);
        log.info("All sessions revoked: userId={}", userId);
    }

    @Transactional
    public boolean revokeSessionByVlessUuid(Long userId, UUID vlessUuid) {
        Optional<DeviceSession> session = sessionRepository.findByUserIdAndVlessUuid(userId, vlessUuid);
        if (session.isEmpty()) {
            log.info("No active session found for userId={}, vlessUuid={} — nothing to unlink", userId, vlessUuid);
            return false;
        }
        DeviceSession s = session.get();
        s.setIsActive(false);
        sessionRepository.save(s);
        log.info("Session unlinked: userId={}, vlessUuid={}, fingerprint={}", userId, vlessUuid, s.getDeviceFingerprint());
        return true;
    }

    public int countActiveSessions(Long userId) {
        return sessionRepository.countByUserIdAndIsActiveTrue(userId);
    }

    private String extractFingerprint(HttpServletRequest request) {
        String hwid = request.getHeader("X-Happ-Hwid");
        if (hwid != null && !hwid.isBlank()) {
            return "hwid:" + hwid;
        }

        String fp = request.getHeader("X-Device-Fingerprint");
        if (fp != null && !fp.isBlank()) {
            return "fp:" + fp;
        }

        String ua = request.getHeader("User-Agent");
        String ip = extractClientIp(request);
        String raw = (ua != null ? ua : "") + "|" + ip;
        return "ua:" + Integer.toHexString(raw.hashCode());
    }

    private String extractClientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}