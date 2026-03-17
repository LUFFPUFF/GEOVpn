package com.vpn.config.repository;


import com.vpn.config.domain.entity.BlockedDomain;
import com.vpn.config.domain.enums.BlockSource;
import com.vpn.config.domain.enums.DomainCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BlockedDomainRepository extends JpaRepository<BlockedDomain, Long> {

    Optional<BlockedDomain> findByDomain(String domain);

    List<BlockedDomain> findByIsBlockedTrue();

    List<BlockedDomain> findByIsBlockedTrueAndCategory(DomainCategory category);

    boolean existsByDomainAndIsBlockedTrue(String domain);

    List<BlockedDomain> findAllBySource(BlockSource source);

    @Modifying
    @Query("DELETE FROM BlockedDomain b WHERE b.source = :source AND b.lastChecked < :cutoffDate")
    int deleteOldDomains(@Param("source") BlockSource source, @Param("cutoffDate") java.time.LocalDateTime cutoffDate);
}
