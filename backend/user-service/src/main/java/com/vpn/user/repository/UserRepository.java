package com.vpn.user.repository;

import com.vpn.user.domain.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByTelegramId(Long telegramId);

    boolean existsByTelegramId(Long telegramId);

    Optional<User> findByReferralCode(String referralCode);

    @Query("SELECT COUNT(u) FROM User u WHERE u.referredBy = :telegramId")
    long countReferrals(Long telegramId);
}
