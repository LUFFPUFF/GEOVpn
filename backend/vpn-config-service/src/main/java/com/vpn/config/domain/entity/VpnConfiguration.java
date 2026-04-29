package com.vpn.config.domain.entity;

import com.vpn.common.dto.enums.ConfigStatus;
import com.vpn.common.dto.enums.ProtocolType;
import com.vpn.config.domain.valueobject.StoredVpnLinks;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * VPN-конфигурация пользователя (одна запись = одно устройство).
 *
 * Ключевые поля:
 * • vlessLinksJson  — список прямых VLESS-ссылок по всем серверам (JSONB)
 * • relayLinksJson  — список relay/антиглушилок (JSONB)
 * • hy2Link         — Hysteria2 ссылка для UDP fallback
 *
 * Ссылки генерируются ОДИН РАЗ при создании/перевыпуске конфига
 * и хранятся «как есть»; подписка просто раздаёт их клиенту.
 */
@Entity
@Table(name = "vpn_configurations")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VpnConfiguration {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "device_id", nullable = false)
    private Long deviceId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "server_id", nullable = false)
    private Integer serverId;

    @Column(name = "vless_uuid", nullable = false, unique = true)
    private UUID vlessUuid;

    /** Основная (primary) ссылка — сохраняется для QR-кода и обратной совместимости */
    @Column(name = "vless_link", nullable = false, columnDefinition = "TEXT")
    private String vlessLink;

    @Column(name = "qr_code_base64", columnDefinition = "TEXT")
    private String qrCodeBase64;


    /**
     * Прямые VLESS-ссылки по всем активным серверам.
     * Тип: List<StoredVpnLinks.DirectLink>
     * Сериализуется/десериализуется через Jackson автоматически.
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "vless_links_json", columnDefinition = "jsonb")
    @Builder.Default
    private List<StoredVpnLinks.DirectLink> vlessLinks = new ArrayList<>();

    /**
     * Ссылки через relay/антиглушилки.
     * Тип: List<StoredVpnLinks.RelayLink>
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "relay_links_json", columnDefinition = "jsonb")
    @Builder.Default
    private List<StoredVpnLinks.RelayLink> relayLinks = new ArrayList<>();

    /**
     * Hysteria2 ссылка (UDP fallback). Может быть null если hy2 не настроен.
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "hy2_links_json", columnDefinition = "jsonb")
    @Builder.Default
    private List<String> hy2Links = new ArrayList<>();

    /** Когда последний раз перестраивались ссылки */
    @Column(name = "links_built_at")
    private LocalDateTime linksBuiltAt;

    /**
     * ОС устройства — определяется при /start в Telegram-боте.
     * Значения: iOS | Android | Windows | macOS | Linux | Unknown
     */
    @Column(name = "device_os", length = 50)
    @Builder.Default
    private String deviceOs = "Unknown";

    /** Человеко-читаемое имя устройства */
    @Column(name = "device_name", length = 255)
    private String deviceName;


    @Enumerated(EnumType.STRING)
    @Column(name = "protocol", nullable = false)
    @Builder.Default
    private ProtocolType protocol = ProtocolType.VLESS;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    @Builder.Default
    private ConfigStatus status = ConfigStatus.ACTIVE;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "revoked_at")
    private LocalDateTime revokedAt;

    @Column(name = "last_used_at")
    private LocalDateTime lastUsedAt;

    public void revoke() {
        this.status = ConfigStatus.REVOKED;
        this.revokedAt = LocalDateTime.now();
    }

    public void suspend() {
        this.status = ConfigStatus.SUSPENDED;
    }

    public void activate() {
        this.status = ConfigStatus.ACTIVE;
        this.revokedAt = null;
    }

    public void updateLastUsed() {
        this.lastUsedAt = LocalDateTime.now();
    }

    public boolean isActive() {
        return this.status == ConfigStatus.ACTIVE;
    }

    /**
     * Возвращает true если ссылки ещё не были сгенерированы
     * (например, после миграции на новую схему).
     */
    public boolean hasNoStoredLinks() {
        return (vlessLinks == null || vlessLinks.isEmpty())
                && (relayLinks == null || relayLinks.isEmpty())
                && (hy2Links == null || hy2Links.isEmpty());
    }

    /**
     * Обновить хранимые ссылки и проставить timestamp пересборки.
     */
    public void storeLinks(
            List<StoredVpnLinks.DirectLink> direct,
            List<StoredVpnLinks.RelayLink> relay,
            List<String> hy2) {

        this.vlessLinks = direct != null ? direct : new ArrayList<>();
        this.relayLinks = relay != null ? relay : new ArrayList<>();
        this.hy2Links = hy2 != null ? hy2 : new ArrayList<>();
        this.linksBuiltAt = LocalDateTime.now();
    }

}