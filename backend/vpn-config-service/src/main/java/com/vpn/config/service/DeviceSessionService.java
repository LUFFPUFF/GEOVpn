package com.vpn.config.service;

import com.vpn.config.domain.entity.DeviceSession;
import com.vpn.config.exception.DeviceLimitExceededException;
import com.vpn.config.repository.DeviceLimitRepository;
import com.vpn.config.repository.DeviceSessionRepository;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Отслеживает физические устройства по HWID из заголовков Happ.
 *
 * Алгоритм при каждом запросе подписки:
 * 1. Извлечь fingerprint из заголовков (X-Happ-Hwid → X-Device-Fingerprint → User-Agent)
 * 2. Если fingerprint уже есть в БД — обновить last_seen_at (не считается новым устройством)
 * 3. Если fingerprint новый — проверить лимит → если не превышен → сохранить новую запись
 * 4. Если лимит превышен → не обновлять подписку (вернуть лимит-блокировку)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DeviceSessionService {

    private final DeviceSessionRepository sessionRepository;
    private final DeviceLimitRepository deviceLimitRepository;
    private final DeviceLimitService deviceLimitService;

    /**
     * Проверяет и регистрирует устройство.
     * @return true если устройство разрешено, false если лимит превышен
     */
    @Transactional
    public boolean checkAndRegisterDevice(
            Long userId,
            UUID vlessUuid,
            HttpServletRequest httpRequest
    ) {
        String fingerprint = extractFingerprint(httpRequest);
        String userAgent   = httpRequest.getHeader("User-Agent");
        String ip          = extractClientIp(httpRequest);
        String deviceName  = httpRequest.getHeader("X-Device-Name");

        if (sessionRepository.existsByUserIdAndDeviceFingerprintAndIsActiveTrue(userId, fingerprint)) {
            sessionRepository.findByUserIdAndDeviceFingerprint(userId, fingerprint)
                    .ifPresent(s -> {
                        s.setLastIp(ip);
                        s.setUserAgent(userAgent);
                        sessionRepository.save(s);
                    });
            log.debug("Known device reconnected: userId={}, fp={}...", userId, fingerprint.substring(0, 8));
            return true;
        }

        int maxDevices     = deviceLimitService.getMaxDevices(userId);
        int activeDevices  = sessionRepository.countByUserIdAndIsActiveTrue(userId);

        if (activeDevices >= maxDevices) {
            log.warn("Device limit exceeded: userId={}, active={}, max={}",
                    userId, activeDevices, maxDevices);
            return false;
        }

        DeviceSession session = DeviceSession.builder()
                .userId(userId)
                .deviceFingerprint(fingerprint)
                .vlessUuid(vlessUuid)
                .userAgent(userAgent)
                .deviceName(deviceName)
                .lastIp(ip)
                .isActive(true)
                .build();

        sessionRepository.save(session);
        log.info("New device registered: userId={}, fp={}..., ip={}",
                userId, fingerprint.substring(0, 8), ip);

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

    public int countActiveSessions(Long userId) {
        return sessionRepository.countByUserIdAndIsActiveTrue(userId);
    }

    /**
     * Извлекает уникальный идентификатор устройства.
     * Приоритет: X-Happ-Hwid → X-Device-Fingerprint → хэш User-Agent + IP
     */
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


