package com.vpn.userservice.repository;

import com.vpn.userservice.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<User, UUID> {
    Optional<User> findByTelegramId(Long telegramId);

    boolean existsByTelegramId(Long telegramId);
}
