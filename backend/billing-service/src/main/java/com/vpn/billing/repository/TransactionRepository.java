package com.vpn.billing.repository;

import com.vpn.billing.domain.entity.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    Optional<Transaction> findByExternalTransactionId(String externalTransactionId);

    List<Transaction> findByUserIdOrderByCreatedAtDesc(Long userId);

    @Query("SELECT t FROM Transaction t WHERE t.createdAt >= :startDate AND t.status = 'COMPLETED' AND t.transactionType = 'DEPOSIT'")
    List<Transaction> findAllCompletedDepositsAfter(@Param("startDate") LocalDateTime startDate);
}
