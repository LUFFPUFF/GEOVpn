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
import java.util.Optional;
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

        Optional<DeviceSession> sessionByFp = sessionRepository.findByUserIdAndDeviceFingerprint(userId, fingerprint);

        if (sessionByFp.isPresent()) {
            DeviceSession session = sessionByFp.get();
            session.setLastIp(ip);
            session.setUserAgent(userAgent);
            session.setVlessUuid(vlessUuid);
            session.setIsActive(true);
            sessionRepository.save(session);

            log.debug("Session updated for fingerprint {}: VlessUuid changed to {}", fingerprint, vlessUuid);
            return true;
        }

        Optional<DeviceSession> sessionByUuid = sessionRepository.findByUserIdAndVlessUuid(userId, vlessUuid);

        if (sessionByUuid.isPresent()) {
            DeviceSession session = sessionByUuid.get();
            session.setLastIp(ip);
            session.setUserAgent(userAgent);
            session.setDeviceFingerprint(fingerprint);
            session.setIsActive(true);
            sessionRepository.save(session);

            log.debug("Session updated for config {}: IP changed to {}", vlessUuid, ip);
            return true;
        }

        int maxDevices = deviceLimitService.getMaxDevices(userId);
        int activeDevices = sessionRepository.countByUserIdAndIsActiveTrue(userId);

        if (activeDevices >= maxDevices) {
            log.warn("Device limit exceeded for userId {}: {}/{}", userId, activeDevices, maxDevices);
            return false;
        }

        try {
            DeviceSession newSession = DeviceSession.builder()
                    .userId(userId)
                    .deviceFingerprint(fingerprint)
                    .vlessUuid(vlessUuid)
                    .userAgent(userAgent)
                    .deviceName(deviceName)
                    .lastIp(ip)
                    .isActive(true)
                    .build();

            sessionRepository.save(newSession);
            log.info("New physical device registered via config {}: ip={}", vlessUuid, ip);
        } catch (org.springframework.dao.DataIntegrityViolationException e) {
            log.warn("Race condition: duplicate device session registration ignored for user {}", userId);
        }

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

    /**
     * Отвязывает физическую сессию по UUID конфигурации.
     * Позволяет пользователю повторно импортировать подписку в Happ
     * после случайного удаления профиля в приложении.
     *
     * @return true если сессия найдена и деактивирована, false если сессии не было
     */
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