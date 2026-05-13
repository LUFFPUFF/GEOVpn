package com.vpn.billing.domain.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.Map;

@Entity
@Table(name = "transactions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Transaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "amount", nullable = false)
    private Integer amount;

    @Column(name = "transaction_type", nullable = false)
    private String transactionType;

    @Column(name = "payment_method")
    private String paymentMethod;

    @Column(name = "status", nullable = false)
    @Builder.Default
    private String status = "PENDING";

    @Column(name = "external_transaction_id")
    private String externalTransactionId;

    @Column(name = "payment_provider")
    private String paymentProvider;

    @Column(name = "description")
    private String description;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "metadata", columnDefinition = "jsonb")
    private Map<String, Object> metadata;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;
}
