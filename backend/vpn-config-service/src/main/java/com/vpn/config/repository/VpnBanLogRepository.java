package com.vpn.config.repository;

import com.vpn.config.domain.entity.VpnBanLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface VpnBanLogRepository extends JpaRepository<VpnBanLog, Long> {

    List<VpnBanLog> findByUserIdAndStatus(Long userId, String status);
}
