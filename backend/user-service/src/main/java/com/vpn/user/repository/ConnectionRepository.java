package com.vpn.user.repository;

import com.vpn.user.domain.entity.Connection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface ConnectionRepository extends JpaRepository<Connection, Long> {

    long countByUserId(Long userId);

    @Query("SELECT SUM(c.bytesSent + c.bytesReceived) FROM Connection c WHERE c.userId = :userId")
    Long sumTotalTrafficByUserId(@Param("userId") Long userId);
}
