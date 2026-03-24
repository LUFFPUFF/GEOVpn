package com.vpn.user.service;

import com.vpn.common.dto.response.AdminDashboardResponse;
import com.vpn.user.repository.DeviceRepository;
import com.vpn.user.repository.UserRepository;
import com.vpn.user.service.interf.AdminService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminServiceImpl implements AdminService {

    private final UserRepository userRepository;
    private final DeviceRepository deviceRepository;

    @Override
    public AdminDashboardResponse getDashboardStats() {
        log.info("Fetching admin dashboard statistics");

        long totalUsers = userRepository.count();
        long activeSubscriptions = deviceRepository.countDistinctUserIdByIsActiveTrue();

        Integer totalBalance = userRepository.sumAllBalances();

        return AdminDashboardResponse.builder()
                .totalUsers(totalUsers)
                .activeSubscriptions(activeSubscriptions)
                .totalServers(5) // TODO: serverRepository.count()
                .activeServers(5) // TODO: serverRepository.countByIsActiveTrue()
                .totalTrafficGb(0L) // TODO: Интеграция с XUI API для получения трафика
                .totalBalanceRub((totalBalance != null ? totalBalance : 0) / 100)
                .build();
    }
}
