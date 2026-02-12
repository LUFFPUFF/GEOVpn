package com.vpn.userservice.repository;

import com.vpn.userservice.entity.Subscription;
import com.vpn.userservice.entity.User;
import com.vpn.userservice.entity.enums.SubscriptionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface SubscriptionRepository extends JpaRepository<Subscription, UUID> {
    List<Subscription> findAllByUserAndStatus(User user, SubscriptionStatus status);
}
