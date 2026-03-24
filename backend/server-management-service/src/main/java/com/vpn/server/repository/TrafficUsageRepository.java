package com.vpn.server.repository;

import com.vpn.common.dto.projection.TrafficSummaryProjection;
import com.vpn.server.domain.entity.TrafficUsage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface TrafficUsageRepository extends JpaRepository<TrafficUsage, Long> {

    List<TrafficUsage> findByUserIdOrderByCollectedAtDesc(Long userId);

    @Query("""
        SELECT 
            COALESCE(SUM(t.bytesIn), 0) as bytesIn, 
            COALESCE(SUM(t.bytesOut), 0) as bytesOut 
        FROM TrafficUsage t 
        WHERE t.userId = :userId
    """)
    TrafficSummaryProjection sumTrafficByUserId(@Param("userId") Long userId);

    @Query("SELECT COALESCE(SUM(t.costKopecks), 0) FROM TrafficUsage t WHERE t.userId = :userId")
    Long sumCostByUserId(@Param("userId") Long userId);

    @Query("""
        SELECT COALESCE(SUM(t.bytesIn + t.bytesOut), 0)
        FROM TrafficUsage t WHERE t.userId = :userId AND t.serverId = :serverId
    """)
    Long sumTrafficByUserAndServer(
            @Param("userId") Long userId,
            @Param("serverId") Integer serverId);
}
