package com.vpn.user.domain.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "telegram_id", nullable = false, unique = true)
    private Long telegramId;

    @Column(name = "username")
    private String username;

    @Column(name = "first_name")
    private String firstName;

    @Column(name = "balance", nullable = false)
    @Builder.Default
    private Integer balance = 0;

    @Column(name = "subscription_type", nullable = false)
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private SubscriptionType subscriptionType = SubscriptionType.PAYG;

    @Column(name = "subscription_expires_at")
    private LocalDateTime subscriptionExpiresAt;

    @Column(name = "referral_code", unique = true)
    private String referralCode;

    @Column(name = "referred_by")
    private Long referredBy;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "last_active_at")
    private LocalDateTime lastActiveAt;

    public boolean hasActiveSubscription() {
        return subscriptionExpiresAt != null &&
                subscriptionExpiresAt.isAfter(LocalDateTime.now());
    }

    public boolean hasSufficientBalance(int amount) {
        return this.balance >= amount;
    }

    public void addBalance(int amount) {
        this.balance += amount;
    }

    public void deductBalance(int amount) {
        if (!hasSufficientBalance(amount)) {
            throw new IllegalStateException("Insufficient balance");
        }
        this.balance -= amount;
    }


}
