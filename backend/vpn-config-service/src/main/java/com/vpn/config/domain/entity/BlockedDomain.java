package com.vpn.config.domain.entity;


import com.vpn.config.domain.enums.BlockSource;
import com.vpn.config.domain.enums.DomainCategory;
import com.vpn.config.domain.enums.MatchType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;


@Entity
@Table(name = "blocked_domains")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BlockedDomain {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 255)
    private String domain;

    @Enumerated(EnumType.STRING)
    @Column(name = "match_type", nullable = false)
    @Builder.Default
    private MatchType matchType = MatchType.EXACT;

    @Column(name = "is_blocked", nullable = false)
    @Builder.Default
    private Boolean isBlocked = true;

    @Column(name = "auto_detected", nullable = false)
    @Builder.Default
    private Boolean autoDetected = false;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private BlockSource source;

    @Enumerated(EnumType.STRING)
    @Column(length = 50)
    private DomainCategory category;

    @Column(name = "last_checked")
    private LocalDateTime lastChecked;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    @PreUpdate
    protected void normalizeData() {
        if (this.domain != null) {
            this.domain = this.domain.trim().toLowerCase();
        }
        if (this.lastChecked == null) {
            this.lastChecked = LocalDateTime.now();
        }
    }
}
