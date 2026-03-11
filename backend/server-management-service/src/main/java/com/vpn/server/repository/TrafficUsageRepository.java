package com.vpn.server.repository;

import com.vpn.server.domain.entity.TrafficUsage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface TrafficUsageRepository extends JpaRepository<TrafficUsage, Long> {

    List<TrafficUsage> findByUserId(Long userId);

    @Query("SELECT SUM(t.bytesTotal) FROM TrafficUsage t WHERE t.userId = :userId AND t.collectedAt > :since")
    Long getTotalBytesSince(@Param("userId") Long userId, @Param("since") LocalDateTime since);

    @Query("SELECT SUM(t.costKopecks) FROM TrafficUsage t WHERE t.userId = :userId")
    Long getTotalSpent(@Param("userId") Long userId);
}
