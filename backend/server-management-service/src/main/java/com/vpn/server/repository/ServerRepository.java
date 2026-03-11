package com.vpn.server.repository;

import com.vpn.server.domain.entity.Server;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ServerRepository extends JpaRepository<Server, Integer> {

    List<Server> findByIsActiveTrue();

    Optional<Server> findByName(String name);
}
