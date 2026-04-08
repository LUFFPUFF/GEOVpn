package com.vpn.user.repository;

import com.vpn.common.dto.response.LeaderboardEntryDto;
import com.vpn.user.domain.entity.User;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByTelegramId(Long telegramId);

    boolean existsByTelegramId(Long telegramId);

    Optional<User> findByReferralCode(String referralCode);

    @Query("SELECT SUM(u.balance) FROM User u")
    Integer sumAllBalances();

    @Query("SELECT COUNT(u) FROM User u WHERE u.referredBy = :telegramId")
    long countReferrals(@Param("telegramId") Long telegramId);

    @Query("SELECT new com.vpn.common.dto.response.LeaderboardEntryDto(r.firstName, r.username, COUNT(u.id), false) " +
            "FROM User u JOIN User r ON u.referredBy = r.telegramId " +
            "WHERE u.createdAt >= :startDate AND u.createdAt < :endDate " +
            "GROUP BY r.telegramId, r.firstName, r.username " +
            "ORDER BY COUNT(u.id) DESC")
    List<LeaderboardEntryDto> getTopReferrals(LocalDateTime startDate, LocalDateTime endDate, Pageable pageable);
}
