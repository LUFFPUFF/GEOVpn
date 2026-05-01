package com.vpn.user.service;

import com.vpn.common.dto.TrafficSessionDto;
import com.vpn.common.dto.TrafficSummaryDto;
import com.vpn.common.dto.response.LeaderboardEntryDto;
import com.vpn.common.dto.response.TrafficStatsResponse;
import com.vpn.common.util.ValidationUtils;
import com.vpn.common.dto.enums.SubscriptionType;
import com.vpn.user.domain.entity.User;
import com.vpn.user.dto.mapper.UserMapper;
import com.vpn.common.dto.request.UserRegistrationRequest;
import com.vpn.common.dto.response.UserResponse;
import com.vpn.common.dto.response.UserStatsResponse;
import com.vpn.common.dto.request.UserUpdateRequest;
import com.vpn.user.exception.DuplicateUserException;
import com.vpn.user.exception.InsufficientBalanceException;
import com.vpn.user.exception.UserNotFoundException;
import com.vpn.user.grpc.TrafficServiceClient;
import com.vpn.user.grpc.VpnServiceClient;
import com.vpn.user.repository.ConnectionRepository;
import com.vpn.user.repository.DeviceRepository;
import com.vpn.user.repository.UserRepository;
import com.vpn.user.service.interf.ReferralService;
import com.vpn.user.service.interf.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final DeviceRepository deviceRepository;
    private final ConnectionRepository connectionRepository;
    private final UserMapper userMapper;
    private final ReferralService referralService;
    private final TrafficServiceClient trafficServiceClient;
    private final VpnServiceClient vpnServiceClient;

    private static final int REGISTRATION_BONUS = 0;

    @Override
    @Transactional
    @CachePut(value = "users", key = "#p0.telegramId")
    public UserResponse registerUser(UserRegistrationRequest request) {
        log.info("Registering new user: telegramId={}", request.getTelegramId());

        ValidationUtils.validateTelegramId(request.getTelegramId());

        if (userRepository.existsByTelegramId(request.getTelegramId())) {
            throw new DuplicateUserException(request.getTelegramId());
        }

        User user = userMapper.toEntity(request);
        user.setBalance(REGISTRATION_BONUS);
        user.setReferralCode(generateUniqueReferralCode());
        user.setSubscriptionType(SubscriptionType.PAYG);

        if (request.getReferralCode() != null && !request.getReferralCode().isEmpty()) {
            referralService.processReferral(user, request.getReferralCode());
        }

        User savedUser = userRepository.save(user);
        log.info("User registered: telegramId={}", savedUser.getTelegramId());
        return userMapper.toResponse(savedUser);
    }

    @Override
    @Cacheable(value = "users", key = "#p0")
    public UserResponse getUserByTelegramId(Long telegramId) {
        ValidationUtils.validateTelegramId(telegramId);
        User user = userRepository.findByTelegramId(telegramId)
                .orElseThrow(() -> new UserNotFoundException(telegramId));
        return userMapper.toResponse(user);
    }

    @Override
    @Cacheable(value = "users", key = "'id:' + #id")
    public UserResponse getUserById(Long id) {
        ValidationUtils.validatePositive(id, "User ID");
        User user = userRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException("User not found with ID: " + id));
        return userMapper.toResponse(user);
    }

    @Override
    @Transactional
    @CachePut(value = "users", key = "#telegramId")
    public UserResponse updateUser(Long telegramId, UserUpdateRequest request) {
        ValidationUtils.validateTelegramId(telegramId);
        User user = userRepository.findByTelegramId(telegramId)
                .orElseThrow(() -> new UserNotFoundException(telegramId));

        if (request.getUsername() != null)  user.setUsername(request.getUsername());
        if (request.getFirstName() != null) user.setFirstName(request.getFirstName());

        return userMapper.toResponse(userRepository.save(user));
    }

    @Override
    @Transactional
    @CacheEvict(value = "users", key = "#telegramId")
    public void deactivateUser(Long telegramId) {
        ValidationUtils.validateTelegramId(telegramId);
        User user = userRepository.findByTelegramId(telegramId)
                .orElseThrow(() -> new UserNotFoundException(telegramId));

        deviceRepository.findByUserIdAndIsActiveTrue(telegramId)
                .forEach(device -> device.setIsActive(false));

        userRepository.save(user);
    }

    @Override
    @Transactional(isolation = Isolation.REPEATABLE_READ)
    @CachePut(value = "users", key = "#p0")
    public UserResponse addBalance(Long telegramId, Integer amount) {
        ValidationUtils.validateTelegramId(telegramId);
        ValidationUtils.validatePositive(amount, "Amount");

        User user = userRepository.findByTelegramId(telegramId)
                .orElseThrow(() -> new UserNotFoundException(telegramId));

        user.addBalance(amount);
        return userMapper.toResponse(userRepository.save(user));
    }

    @Override
    @Transactional(isolation = Isolation.REPEATABLE_READ)
    @CachePut(value = "users", key = "#p0")
    public UserResponse deductBalance(Long telegramId, Integer amount) {
        ValidationUtils.validateTelegramId(telegramId);
        ValidationUtils.validatePositive(amount, "Amount");

        User user = userRepository.findByTelegramId(telegramId)
                .orElseThrow(() -> new UserNotFoundException(telegramId));

        if (!user.hasSufficientBalance(amount)) {
            throw new InsufficientBalanceException(amount, user.getBalance());
        }

        user.deductBalance(amount);
        return userMapper.toResponse(userRepository.save(user));
    }

    @Override
    @Cacheable(value = "user-stats", key = "#telegramId", unless = "#result == null")
    public UserStatsResponse getUserStats(Long telegramId) {
        ValidationUtils.validateTelegramId(telegramId);

        User user = userRepository.findByTelegramId(telegramId)
                .orElseThrow(() -> new UserNotFoundException(telegramId));

        long activeDevices    = deviceRepository.countActiveDevicesByUserId(telegramId);
        long totalDevices     = deviceRepository.countByUserId(telegramId);
        long totalReferrals   = userRepository.countReferrals(telegramId);
        long totalConnections = connectionRepository.countByUserId(telegramId);

        TrafficSummaryDto summary = trafficServiceClient.getTrafficSummary(telegramId).getData();
        long bytesIn      = summary != null && summary.getBytesIn()     != null ? summary.getBytesIn()     : 0L;
        long bytesOut     = summary != null && summary.getBytesOut()    != null ? summary.getBytesOut()    : 0L;
        long spentKopecks = summary != null && summary.getCostKopecks() != null ? summary.getCostKopecks() : 0L;

        return UserStatsResponse.builder()
                .telegramId(telegramId)
                .balance(user.getBalance())
                .totalDevices((int) totalDevices)
                .activeDevices((int) activeDevices)
                .totalReferrals(totalReferrals)
                .totalReferralEarnings(0)
                .totalConnections(totalConnections)
                .totalTrafficBytes(bytesIn + bytesOut)
                .totalSpentKopecks(spentKopecks)
                .build();
    }

    @Override
    public TrafficStatsResponse getTrafficStats(Long telegramId) {
        ValidationUtils.validateTelegramId(telegramId);

        if (!userRepository.existsByTelegramId(telegramId)) {
            throw new UserNotFoundException(telegramId);
        }

        TrafficSummaryDto summary              = trafficServiceClient.getTrafficSummary(telegramId).getData();
        List<TrafficSessionDto> recentSessions = trafficServiceClient.getRecentSessions(telegramId).getData();

        long bytesIn  = summary != null && summary.getBytesIn()     != null ? summary.getBytesIn()     : 0L;
        long bytesOut = summary != null && summary.getBytesOut()    != null ? summary.getBytesOut()    : 0L;
        long cost     = summary != null && summary.getCostKopecks() != null ? summary.getCostKopecks() : 0L;
        long total    = bytesIn + bytesOut;

        List<TrafficStatsResponse.TrafficSession> sessions = recentSessions.stream()
                .limit(50)
                .map(t -> TrafficStatsResponse.TrafficSession.builder()
                        .serverId(t.getServerId())
                        .bytesIn(t.getBytesIn())
                        .bytesOut(t.getBytesOut())
                        .bytesTotal(t.getBytesIn() + t.getBytesOut())
                        .costKopecks(t.getCostKopecks())
                        .collectedAt(t.getCollectedAt())
                        .build())
                .toList();

        return TrafficStatsResponse.builder()
                .userId(telegramId)
                .totalBytesIn(bytesIn)
                .totalBytesOut(bytesOut)
                .totalBytes(total)
                .totalGb(total / (1024.0 * 1024.0 * 1024.0))
                .totalCostKopecks(cost)
                .totalCostRubles(cost / 100.0)
                .sessions(sessions)
                .build();
    }

    /**
     * Оформление подписки (без промо) — делегирует к основному методу.
     */
    @Override
    public UserResponse purchaseSubscription(Long telegramId, String planName, int months) {
        return purchaseSubscription(telegramId, planName, months, false);
    }

    @Override
    @Transactional(isolation = Isolation.REPEATABLE_READ)
    @CacheEvict(value = "users", key = "#telegramId")
    public UserResponse purchaseSubscription(Long telegramId, String planName, int months, boolean promo) {
        log.info("purchaseSubscription: user={}, plan={}, months={}, promo={}", telegramId, planName, months, promo);

        User user = userRepository.findByTelegramId(telegramId)
                .orElseThrow(() -> new UserNotFoundException(telegramId));

        if (promo && "BASIC".equalsIgnoreCase(planName)) {
            if (user.getSubscriptionType() != SubscriptionType.PAYG) {
                log.warn("Promo denied: user {} already has subscription {}", telegramId, user.getSubscriptionType());
                return userMapper.toResponse(user);
            }

            log.info("Granting FREE promo BASIC month to user {}", telegramId);

            user.setSubscriptionType(SubscriptionType.BASIC);
            user.extendSubscription(1, SubscriptionType.BASIC);

            User savedUser = userRepository.saveAndFlush(user);
            log.info("Saved user {} with subscriptionType={}", telegramId, savedUser.getSubscriptionType());

            syncVpnLimits(telegramId, "BASIC", savedUser);
            return userMapper.toResponse(savedUser);
        }

        int totalCost = getPlanCost(planName) * months;

        if (totalCost > 0) {
            if (!user.hasSufficientBalance(totalCost)) {
                throw new InsufficientBalanceException(totalCost, user.getBalance());
            }
            user.deductBalance(totalCost);
        }

        SubscriptionType type;
        try {
            type = SubscriptionType.valueOf(planName.toUpperCase());
        } catch (IllegalArgumentException e) {
            type = SubscriptionType.BASIC;
        }

        user.setSubscriptionType(type);
        user.extendSubscription(months, type);

        User savedUser = userRepository.saveAndFlush(user);
        syncVpnLimits(telegramId, planName, savedUser);

        return userMapper.toResponse(savedUser);
    }


    @Override
    public boolean userExists(Long telegramId) {
        ValidationUtils.validateTelegramId(telegramId);
        return userRepository.existsByTelegramId(telegramId);
    }

    @Override
    public List<UserResponse> getAllUsers(int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return userRepository.findAll(pageable).stream()
                .map(userMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    @CacheEvict(value = "users", key = "#telegramId")
    public void updateLastActive(Long telegramId) {
        userRepository.findByTelegramId(telegramId).ifPresent(user -> {
            user.setLastActiveAt(LocalDateTime.now());
            userRepository.save(user);
        });
    }

    @Override
    public List<LeaderboardEntryDto> getLeaderboard() {
        LocalDateTime now = LocalDateTime.now();
        boolean isWinnerDay = now.getDayOfMonth() == 1;

        if (isWinnerDay) {
            LocalDateTime start = now.minusMonths(1).withDayOfMonth(1).toLocalDate().atStartOfDay();
            LocalDateTime end   = now.withDayOfMonth(1).toLocalDate().atStartOfDay();
            List<LeaderboardEntryDto> top = userRepository.getTopReferrals(start, end, PageRequest.of(0, 1));
            if (!top.isEmpty()) top.getFirst().setWinner(true);
            return top;
        }

        LocalDateTime start = now.withDayOfMonth(1).toLocalDate().atStartOfDay();
        LocalDateTime end   = now.plusMonths(1).withDayOfMonth(1).toLocalDate().atStartOfDay();
        return userRepository.getTopReferrals(start, end, PageRequest.of(0, 10));
    }

    private void syncVpnLimits(Long telegramId, String planName, User savedUser) {
        try {
            String expiresAtStr = savedUser.getSubscriptionExpiresAt() != null
                    ? savedUser.getSubscriptionExpiresAt().toString()
                    : "";

            vpnServiceClient.setDeviceLimit(telegramId, Map.of(
                    "maxDevices", getMaxDevicesForPlan(planName),
                    "planName",   savedUser.getSubscriptionType().name(),
                    "expiresAt",  expiresAtStr
            ));
        } catch (Exception e) {
            log.error("Failed to sync VPN limits for user {}", telegramId, e);
        }
    }

    private String generateUniqueReferralCode() {
        String code;
        int attempts = 0;
        do {
            code = UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase();
            if (++attempts > 5) throw new IllegalStateException("Unable to generate unique referral code");
        } while (userRepository.findByReferralCode(code).isPresent());
        return code;
    }

    private int getPlanCost(String planName) {
        return switch (planName.toUpperCase()) {
            case "BASIC"     -> 15000;
            case "STANDARD"  -> 40000;
            case "FAMILY"    -> 70000;
            case "BUSINESS"  -> 200000;
            case "UNLIMITED" -> 300000;
            default -> throw new IllegalArgumentException("Unknown subscription plan: " + planName);
        };
    }

    private int getMaxDevicesForPlan(String planName) {
        return switch (planName.toUpperCase()) {
            case "BASIC"     -> 1;
            case "STANDARD"  -> 3;
            case "FAMILY"    -> 5;
            case "BUSINESS"  -> 15;
            case "UNLIMITED" -> 100;
            default          -> 1;
        };
    }
}