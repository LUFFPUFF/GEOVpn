package com.vpn.user.service;

import com.vpn.common.dto.request.ConnectionUpdateRequest;
import com.vpn.user.domain.entity.Connection;
import com.vpn.user.repository.ConnectionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class ConnectionService {

    private final ConnectionRepository connectionRepository;

    /**
     * Открывает новую сессию или обновляет байты в существующей
     */
    @Transactional
    public void updateConnection(ConnectionUpdateRequest req) {
        connectionRepository.findOpenSession(
                        req.getUserId(), req.getServerId(), req.getDeviceId())
                .ifPresentOrElse(
                        conn -> {
                            conn.setBytesSent(
                                    nullSafe(conn.getBytesSent()) + req.getDeltaUp());
                            conn.setBytesReceived(
                                    nullSafe(conn.getBytesReceived()) + req.getDeltaDown());
                            connectionRepository.save(conn);
                            log.debug("Updated connection id={} +{}↑ +{}↓",
                                    conn.getId(), req.getDeltaUp(), req.getDeltaDown());
                        },
                        () -> {
                            Connection conn = Connection.builder()
                                    .userId(req.getUserId())
                                    .deviceId(req.getDeviceId())
                                    .serverId(req.getServerId())
                                    .bytesSent(req.getDeltaUp())
                                    .bytesReceived(req.getDeltaDown())
                                    .connectedAt(LocalDateTime.now())
                                    .build();
                            connectionRepository.save(conn);
                            log.info("New connection opened: userId={}, serverId={}",
                                    req.getUserId(), req.getServerId());
                        }
                );
    }

    /**
     * Закрывает открытую сессию
     */
    @Transactional
    public void closeConnection(ConnectionUpdateRequest req) {
        connectionRepository.findOpenSession(
                        req.getUserId(), req.getServerId(), req.getDeviceId())
                .ifPresent(conn -> {
                    conn.setDisconnectedAt(LocalDateTime.now());
                    connectionRepository.save(conn);
                    log.info("Connection closed: id={}, userId={}",
                            conn.getId(), req.getUserId());
                });
    }

    private long nullSafe(Long value) {
        return value != null ? value : 0L;
    }
}
