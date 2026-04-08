package com.vpn.user.service;

import com.vpn.common.dto.TrafficSessionDto;
import com.vpn.common.dto.TrafficSummaryDto;
import com.vpn.common.dto.request.ConfigCreateRequest;
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
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Реализация UserService с полной бизнес-логикой
 *
 * Features:
 * - Транзакционность
 * - Кеширование в Redis
 * - Реферальная система
 * - Pessimistic locking для операций с балансом
 * - Валидации
 */

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

    private static final int REGISTRATION_BONUS = 5000; //todo нужно ли?

    private static final int MONTHLY_PLAN_COST = 5000;

    /**
     * Регистрация нового пользователя
     *
     * @param request данные для регистрации
     * @return зарегистрированный пользователь
     * @throws DuplicateUserException если пользователь уже существует
     */
    @Override
    @Transactional
    @CachePut(value = "users", key = "#p0.telegramId")
    public UserResponse registerUser(UserRegistrationRequest request) {
        log.info("Registering new user with Telegram ID: {}", request.getTelegramId());

        ValidationUtils.validateTelegramId(request.getTelegramId());

        if (userRepository.existsByTelegramId(request.getTelegramId())) {
            log.warn("User already exists with Telegram ID: {}", request.getTelegramId());
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

        log.info("User registered successfully: telegramId={}, referralCode={}",
                savedUser.getTelegramId(), savedUser.getReferralCode());

        return userMapper.toResponse(savedUser);
    }

    /**
     * Получить пользователя по Telegram ID
     * Результат кешируется в Redis
     */
    @Override
    @Cacheable(value = "users", key = "#p0")
    public UserResponse getUserByTelegramId(Long telegramId) {
        log.debug("Fetching user by Telegram ID: {}", telegramId);

        ValidationUtils.validateTelegramId(telegramId);

        User user = userRepository.findByTelegramId(telegramId)
                .orElseThrow(() -> new UserNotFoundException(telegramId));

        return userMapper.toResponse(user);
    }

    /**
     * Получить пользователя по внутреннему ID
     */
    @Override
    @Cacheable(value = "users", key = "'id:' + #id")
    public UserResponse getUserById(Long id) {
        log.debug("Fetching user by ID: {}", id);

        ValidationUtils.validatePositive(id, "User ID");

        User user = userRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException("User not found with ID: " + id));

        return userMapper.toResponse(user);
    }

    /**
     * Обновить профиль пользователя
     */
    @Override
    @Transactional
    @CachePut(value = "users", key = "#telegramId")
    public UserResponse updateUser(Long telegramId, UserUpdateRequest request) {
        log.info("Updating user profile: telegramId={}", telegramId);

        ValidationUtils.validateTelegramId(telegramId);

        User user = userRepository.findByTelegramId(telegramId)
                .orElseThrow(() -> new UserNotFoundException(telegramId));

        if (request.getUsername() != null) {
            user.setUsername(request.getUsername());
        }
        if (request.getFirstName() != null) {
            user.setFirstName(request.getFirstName());
        }

        User updatedUser = userRepository.save(user);

        log.info("User profile updated: telegramId={}", telegramId);

        return userMapper.toResponse(updatedUser);
    }

    /**
     * Деактивировать пользователя (soft delete)
     */
    @Override
    @Transactional
    @CacheEvict(value = "users", key = "#telegramId")
    public void deactivateUser(Long telegramId) {
        log.warn("Deactivating user: telegramId={}", telegramId);

        ValidationUtils.validateTelegramId(telegramId);

        User user = userRepository.findByTelegramId(telegramId)
                .orElseThrow(() -> new UserNotFoundException(telegramId));

        // TODO: Добавить поле is_active в User entity
        // user.setActive(false);

        deviceRepository.findByUserIdAndIsActiveTrue(telegramId)
                .forEach(device -> device.setIsActive(false));

        userRepository.save(user);

        log.info("User deactivated: telegramId={}", telegramId);
    }

    /**
     * Добавить баланс пользователю
     * Используется при пополнении или начислении бонусов
     */
    @Override
    @Transactional(isolation = Isolation.REPEATABLE_READ)
    @CachePut(value = "users", key = "#p0")
    public UserResponse addBalance(Long telegramId, Integer amount) {
        log.info("Adding balance: telegramId={}, amount={}", telegramId, amount);

        ValidationUtils.validateTelegramId(telegramId);
        ValidationUtils.validatePositive(amount, "Amount");

        User user = userRepository.findByTelegramId(telegramId)
                .orElseThrow(() -> new UserNotFoundException(telegramId));

        int oldBalance = user.getBalance();
        user.addBalance(amount);

        User updatedUser = userRepository.save(user);

        log.info("Balance added: telegramId={}, oldBalance={}, newBalance={}, added={}",
                telegramId, oldBalance, updatedUser.getBalance(), amount);

        return userMapper.toResponse(updatedUser);
    }

    /**
     * Списать баланс пользователя
     * Используется при оплате или использовании услуг
     *
     * CRITICAL: Используем PESSIMISTIC_WRITE lock для предотвращения race conditions
     */
    @Override
    @Transactional(isolation = Isolation.REPEATABLE_READ)
    @CachePut(value = "users", key = "#p0")
    public UserResponse deductBalance(Long telegramId, Integer amount) {
        log.info("Deducting balance: telegramId={}, amount={}", telegramId, amount);

        ValidationUtils.validateTelegramId(telegramId);
        ValidationUtils.validatePositive(amount, "Amount");

        User user = userRepository.findByTelegramId(telegramId)
                .orElseThrow(() -> new UserNotFoundException(telegramId));

        if (user.hasSufficientBalance(amount)) {
            log.warn("Insufficient balance: telegramId={}, required={}, available={}",
                    telegramId, amount, user.getBalance());
            throw new InsufficientBalanceException(amount, user.getBalance());
        }

        int oldBalance = user.getBalance();
        user.deductBalance(amount);

        User updatedUser = userRepository.save(user);

        log.info("Balance deducted: telegramId={}, oldBalance={}, newBalance={}, deducted={}",
                telegramId, oldBalance, updatedUser.getBalance(), amount);

        return userMapper.toResponse(updatedUser);
    }

    /**
     * Получить статистику пользователя
     */
    @Override
    @Cacheable(value = "user-stats", key = "#telegramId", unless = "#result == null")
    public UserStatsResponse getUserStats(Long telegramId) {
        log.debug("Fetching user stats: telegramId={}", telegramId);

        ValidationUtils.validateTelegramId(telegramId);

        User user = userRepository.findByTelegramId(telegramId)
                .orElseThrow(() -> new UserNotFoundException(telegramId));

        long activeDevices = deviceRepository.countActiveDevicesByUserId(telegramId);
        long totalDevices  = deviceRepository.countByUserId(telegramId);
        long totalReferrals = userRepository.countReferrals(telegramId);
        long totalConnections = connectionRepository.countByUserId(telegramId);

        TrafficSummaryDto summary = trafficServiceClient.getTrafficSummary(telegramId).getData();

        long totalBytesIn = summary != null && summary.getBytesIn() != null ? summary.getBytesIn() : 0L;
        long totalBytesOut = summary != null && summary.getBytesOut() != null ? summary.getBytesOut() : 0L;
        long totalTraffic = totalBytesIn + totalBytesOut;
        long totalSpentKopecks = summary != null && summary.getCostKopecks() != null ? summary.getCostKopecks() : 0L;

        return UserStatsResponse.builder()
                .telegramId(telegramId)
                .balance(user.getBalance())
                .totalDevices((int) totalDevices)
                .activeDevices((int) activeDevices)
                .totalReferrals(totalReferrals)
                .totalReferralEarnings(0)
                .totalConnections(totalConnections)
                .totalTrafficBytes(totalTraffic)
                .totalSpentKopecks(totalSpentKopecks)
                .build();
    }

    public TrafficStatsResponse getTrafficStats(Long telegramId) {
        log.debug("Fetching traffic stats: telegramId={}", telegramId);

        ValidationUtils.validateTelegramId(telegramId);

        if (!userRepository.existsByTelegramId(telegramId)) {
            throw new UserNotFoundException(telegramId);
        }

        TrafficSummaryDto summary = trafficServiceClient.getTrafficSummary(telegramId).getData();
        List<TrafficSessionDto> recentSessions = trafficServiceClient.getRecentSessions(telegramId).getData();

        long bytesIn = summary != null && summary.getBytesIn() != null ? summary.getBytesIn() : 0L;
        long bytesOut = summary != null && summary.getBytesOut() != null ? summary.getBytesOut() : 0L;
        long total = bytesIn + bytesOut;
        long cost = summary != null && summary.getCostKopecks() != null ? summary.getCostKopecks() : 0L;

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

    @Override
    @Transactional(isolation = Isolation.REPEATABLE_READ)
    @CacheEvict(value = "users", key = "#telegramId")
    public UserResponse purchaseSubscription(Long telegramId, String planName, int months) {
        log.info("Processing subscription: user={}, plan={}, months={}", telegramId, planName, months);

        ValidationUtils.validateTelegramId(telegramId);
        if (months <= 0) throw new IllegalArgumentException("Months must be positive");

        User user = userRepository.findByTelegramId(telegramId)
                .orElseThrow(() -> new UserNotFoundException(telegramId));

        int costPerMonth = getPlanCost(planName);
        int totalCost = costPerMonth * months;

        if (!user.hasSufficientBalance(totalCost)) {
            log.warn("Purchase failed: insufficient funds. User {}, Cost {}, Balance {}",
                    telegramId, totalCost, user.getBalance());
            throw new InsufficientBalanceException(totalCost, user.getBalance());
        }

        user.deductBalance(totalCost);

        SubscriptionType type = SubscriptionType.valueOf(planName.toUpperCase());
        user.extendSubscription(months, type);
        User savedUser = userRepository.save(user);

        try {
            int maxDevices = getMaxDevicesForPlan(planName);

            java.util.Map<String, Object> limitRequest = java.util.Map.of(
                    "maxDevices", maxDevices,
                    "planName", planName.toUpperCase(),
                    "expiresAt", savedUser.getSubscriptionExpiresAt().toString()
            );

            vpnServiceClient.setDeviceLimit(telegramId, limitRequest);
            log.info("Device limits updated successfully via Feign for user {}", telegramId);
        } catch (Exception e) {
            log.error("CRITICAL: Failed to sync device limit for user {} with vpn-config-service: {}", telegramId, e.getMessage());
        }

        try {
            ConfigCreateRequest configRequest = ConfigCreateRequest.builder()
                    .userId(telegramId)
                    .userTelegramId(telegramId)
                    .deviceId(1L)
                    .protocol("VLESS")
                    .preferredCountry("NL")
                    .build();

            vpnServiceClient.createConfig(telegramId, configRequest);
            log.info("Initial VPN config provisioned for user {}", telegramId);
        } catch (Exception e) {
            log.error("Failed to provision initial config for user {}", telegramId, e);
        }

        log.info("Subscription success: user={}, plan={}, expiresAt={}",
                telegramId, planName, savedUser.getSubscriptionExpiresAt());

        return userMapper.toResponse(savedUser);
    }

    /**
     * Проверить существование пользователя
     */
    @Override
    public boolean userExists(Long telegramId) {
        ValidationUtils.validateTelegramId(telegramId);
        return userRepository.existsByTelegramId(telegramId);
    }

    /**
     * Получить всех пользователей с пагинацией
     */
    @Override
    public List<UserResponse> getAllUsers(int page, int size) {
        log.debug("Fetching all users: page={}, size={}", page, size);

        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());

        return userRepository.findAll(pageable)
                .stream()
                .map(userMapper::toResponse)
                .collect(Collectors.toList());
    }

    /**
     * Обновить время последней активности
     */
    @Override
    @Transactional
    @CacheEvict(value = "users", key = "#telegramId")
    public void updateLastActive(Long telegramId) {
        log.trace("Updating last active time: telegramId={}", telegramId);

        userRepository.findByTelegramId(telegramId)
                .ifPresent(user -> {
                    user.setLastActiveAt(LocalDateTime.now());
                    userRepository.save(user);
                });
    }

    @Override
    public List<LeaderboardEntryDto> getLeaderboard() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime startDate;
        LocalDateTime endDate;
        boolean isWinnerDay = now.getDayOfMonth() == 1;

        if (isWinnerDay) {
            startDate = now.minusMonths(1).withDayOfMonth(1).toLocalDate().atStartOfDay();
            endDate = now.withDayOfMonth(1).toLocalDate().atStartOfDay();

            List<LeaderboardEntryDto> top = userRepository.getTopReferrals(startDate, endDate, PageRequest.of(0, 1));
            if (!top.isEmpty()) {
                top.getFirst().setWinner(true);
            }
            return top;
        } else {
            startDate = now.withDayOfMonth(1).toLocalDate().atStartOfDay();
            endDate = now.plusMonths(1).withDayOfMonth(1).toLocalDate().atStartOfDay();

            return userRepository.getTopReferrals(startDate, endDate, PageRequest.of(0, 10));
        }
    }

    /**
     * Генерация уникального реферального кода
     */
    private String generateUniqueReferralCode() {
        String code;
        int attempts = 0;
        int maxAttempts = 5;

        do {
            code = generateShortCode();
            attempts++;

            if (attempts > maxAttempts) {
                log.error("Failed to generate unique referral code after {} attempts", maxAttempts);
                throw new IllegalStateException("Unable to generate unique referral code");
            }
        } while (userRepository.findByReferralCode(code).isPresent());

        return code;
    }

    /**
     * Генерация короткого кода (8 символов)
     */
    private String generateShortCode() {
        return UUID.randomUUID()
                .toString()
                .replace("-", "")
                .substring(0, 8)
                .toUpperCase();
    }

    private int getPlanCost(String planName) {
        return switch (planName.toUpperCase()) {
            case "BASIC" -> 15000;
            case "STANDARD" -> 40000;
            case "FAMILY" -> 70000;
            case "BUSINESS" -> 200000;
            case "UNLIMITED" -> 300000;
            default -> throw new IllegalArgumentException("Unknown subscription plan: " + planName);
        };
    }

    private int getMaxDevicesForPlan(String planName) {
        return switch (planName.toUpperCase()) {
            case "BASIC" -> 1;
            case "STANDARD" -> 3;
            case "FAMILY" -> 5;
            case "BUSINESS" -> 15;
            case "UNLIMITED" -> 100;
            default -> 1;
        };
    }
}
