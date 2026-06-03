package com.vpn.user.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.vpn.common.dto.TrafficSessionDto;
import com.vpn.common.dto.TrafficSummaryDto;
import com.vpn.common.dto.response.*;
import com.vpn.common.util.ValidationUtils;
import com.vpn.common.dto.enums.SubscriptionType;
import com.vpn.user.domain.entity.User;
import com.vpn.user.dto.mapper.UserMapper;
import com.vpn.common.dto.request.UserRegistrationRequest;
import com.vpn.common.dto.request.UserUpdateRequest;
import com.vpn.user.exception.ApplyPromoCodeException;
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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
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
    private final RestTemplate restTemplate;

    private final ExecutorService executorService = Executors.newVirtualThreadPerTaskExecutor();

    @org.springframework.context.annotation.Lazy
    @org.springframework.beans.factory.annotation.Autowired
    private UserService self;

    private static final int REGISTRATION_BONUS = 0;

    @Value("${service.bot-token}")
    private String botToken;

    @Value("${service.channel-id}")
    private String channelId;

    @Override
    @Transactional
    @CachePut(value = "users", key = "#p0.telegramId")
    public UserResponse registerUser(UserRegistrationRequest request) {
        log.info("Registering new user: telegramId={}", request.getTelegramId());

        ValidationUtils.validateTelegramId(request.getTelegramId());

        try {
            if (userRepository.existsByTelegramId(request.getTelegramId())) {
                log.info("User already exists, skipping registration: telegramId={}", request.getTelegramId());
                return userMapper.toResponse(userRepository.findByTelegramId(request.getTelegramId()).get());
            }

            User user = userMapper.toEntity(request);
            user.setBalance(REGISTRATION_BONUS);
            user.setReferralCode(generateUniqueReferralCode());
            user.setSubscriptionType(SubscriptionType.PAYG);

            if (request.getReferralCode() != null && !request.getReferralCode().trim().isEmpty()) {
                String refCode = request.getReferralCode().toUpperCase().trim();

                Optional<User> referrerOpt = userRepository.findByReferralCode(refCode);

                if (referrerOpt.isEmpty() && refCode.matches("^\\d+$")) {
                    try {
                        Long refTelegramId = Long.parseLong(refCode);
                        referrerOpt = userRepository.findByTelegramId(refTelegramId);
                    } catch (NumberFormatException ignored) {}
                }

                referrerOpt.ifPresent(referrer -> {
                    if (!referrer.getTelegramId().equals(user.getTelegramId())) {
                        user.setReferredBy(referrer.getTelegramId());
                        user.setPromoApplied(true);

                        referrer.addBalance(5000);
                        userRepository.save(referrer);
                        log.info("Referral: Added 5000 kopecks to referrer {}", referrer.getTelegramId());

                        user.setSubscriptionType(SubscriptionType.BASIC);
                        user.setSubscriptionExpiresAt(LocalDateTime.now().plusDays(10));
                    }
                });

                try {
                    referralService.processReferral(user, request.getReferralCode());
                } catch (Exception e) {
                    log.error("Error processing referral in referralService: {}", e.getMessage());
                }
            }

            User savedUser = userRepository.saveAndFlush(user);
            log.info("User registered: telegramId={}", savedUser.getTelegramId());

            if (savedUser.getSubscriptionType() != SubscriptionType.PAYG) {
                syncVpnLimits(savedUser.getTelegramId(), savedUser.getSubscriptionType().name(), savedUser);
            }

            return userMapper.toResponse(savedUser);

        } catch (org.springframework.dao.DataIntegrityViolationException e) {
            log.warn("Параллельная коллизия регистрации для пользователя {}. Возвращаем существующего пользователя.", request.getTelegramId());
            User existingUser = userRepository.findByTelegramId(request.getTelegramId())
                    .orElseThrow(() -> new RuntimeException("Ошибка конкурентной регистрации: пользователь не найден после конфликта", e));
            return userMapper.toResponse(existingUser);
        }
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
    @Caching(evict = {
            @CacheEvict(value = "users", key = "#p0"),
            @CacheEvict(value = "user-max-devices", key = "#p0"),
            @CacheEvict(value = "user-stats", key = "#p0")
    })
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
    @Caching(evict = {
            @CacheEvict(value = "users", key = "#p0"),
            @CacheEvict(value = "user-max-devices", key = "#p0"),
            @CacheEvict(value = "user-stats", key = "#p0")
    })
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
    @Transactional(isolation = Isolation.REPEATABLE_READ)
    @CacheEvict(value = {"users", "user-max-devices"}, key = "#telegramId")
    public UserResponse applyPromoCode(Long telegramId, String code) {
        log.info("User {} applying promo code: {}", telegramId, code);

        User currentUser = userRepository.findByTelegramId(telegramId)
                .orElseThrow(() -> new UserNotFoundException(telegramId));

        if (currentUser.isPromoApplied()) {
            throw new ApplyPromoCodeException("Вы уже применяли промокод ранее");
        }

        if (code.equalsIgnoreCase(currentUser.getReferralCode())) {
            throw new ApplyPromoCodeException("Вы не можете применить свой собственный код");
        }

        User referrer = userRepository.findByReferralCode(code.toUpperCase())
                .orElseThrow(() -> new RuntimeException("Промокод не найден или недействителен"));

        currentUser.setPromoApplied(true);

        currentUser.setReferredBy(referrer.getTelegramId());

        referrer.addBalance(5000);
        userRepository.save(referrer);
        log.info("Added 50 RUB to referrer {}", referrer.getTelegramId());

        if (currentUser.getSubscriptionType() == SubscriptionType.PAYG) {
            currentUser.setSubscriptionType(SubscriptionType.BASIC);
        }

        LocalDateTime currentExpiry = currentUser.getSubscriptionExpiresAt();
        if (currentExpiry == null || currentExpiry.isBefore(LocalDateTime.now())) {
            currentUser.setSubscriptionExpiresAt(LocalDateTime.now().plusDays(10));
        } else {
            currentUser.setSubscriptionExpiresAt(currentExpiry.plusDays(10));
        }

        User savedUser = userRepository.saveAndFlush(currentUser);

        syncVpnLimits(telegramId, savedUser.getSubscriptionType().name(), savedUser);

        return userMapper.toResponse(savedUser);
    }

    @Override
    public List<Long> getAllActiveUserIds() {
        return userRepository.findAllActiveUserIds(LocalDateTime.now());
    }

    @Override
    @Transactional
    @CachePut(value = "users", key = "#telegramId")
    public UserResponse updateReferralCode(Long telegramId, String newCode) {
        log.info("User {} changing referral code to: {}", telegramId, newCode);

        if (newCode == null || !newCode.matches("^[A-Z0-9]{3,15}$")) {
            throw new RuntimeException("Код должен содержать от 3 до 15 латинских букв и цифр");
        }

        User user = userRepository.findByTelegramId(telegramId)
                .orElseThrow(() -> new UserNotFoundException(telegramId));

        if (user.getReferralCode().equalsIgnoreCase(newCode)) {
            return userMapper.toResponse(user);
        }

        if (userRepository.findByReferralCode(newCode).isPresent()) {
            throw new RuntimeException("Этот промокод уже занят! Выберите другой.");
        }

        user.setReferralCode(newCode);
        return userMapper.toResponse(userRepository.save(user));
    }

    @Override
    @Transactional
    @CachePut(value = "users", key = "#telegramId")
    public UserResponse claimEasterEgg(Long telegramId) {
        log.info("User {} is claiming easter egg", telegramId);

        User user = userRepository.findByTelegramId(telegramId)
                .orElseThrow(() -> new UserNotFoundException(telegramId));

        if (user.isEasterEggClaimed()) {
            throw new RuntimeException("Вы уже получили этот подарок!");
        }

        user.setEasterEggClaimed(true);

        if (user.getSubscriptionType() == SubscriptionType.PAYG) {
            user.setSubscriptionType(SubscriptionType.BASIC);
        }

        LocalDateTime currentExpiry = user.getSubscriptionExpiresAt();
        if (currentExpiry == null || currentExpiry.isBefore(LocalDateTime.now())) {
            user.setSubscriptionExpiresAt(LocalDateTime.now().plusDays(2));
        } else {
            user.setSubscriptionExpiresAt(currentExpiry.plusDays(2));
        }

        User savedUser = userRepository.saveAndFlush(user);

        syncVpnLimits(telegramId, savedUser.getSubscriptionType().name(), savedUser);

        return userMapper.toResponse(savedUser);
    }

    @Override
    @Cacheable(value = "user-stats", key = "#telegramId", unless = "#result == null")
    public UserStatsResponse getUserStats(Long telegramId) {
        ValidationUtils.validateTelegramId(telegramId);

        User user = userRepository.findByTelegramId(telegramId)
                .orElseThrow(() -> new UserNotFoundException(telegramId));

        CompletableFuture<Long> activeDevicesFuture = CompletableFuture.supplyAsync(
                () -> deviceRepository.countActiveDevicesByUserId(telegramId), executorService);

        CompletableFuture<Long> totalDevicesFuture = CompletableFuture.supplyAsync(
                () -> deviceRepository.countByUserId(telegramId), executorService);

        CompletableFuture<Long> totalReferralsFuture = CompletableFuture.supplyAsync(
                () -> userRepository.countReferrals(telegramId), executorService);

        CompletableFuture<Long> totalConnectionsFuture = CompletableFuture.supplyAsync(
                () -> connectionRepository.countByUserId(telegramId), executorService);

        CompletableFuture<TrafficSummaryDto> trafficSummaryFuture = CompletableFuture.supplyAsync(
                () -> {
                    try {
                        return trafficServiceClient.getTrafficSummary(telegramId).getData();
                    } catch (Exception e) {
                        log.warn("Failed to fetch traffic summary for user {}", telegramId, e);
                        return null;
                    }
                }, executorService);

        CompletableFuture.allOf(
                activeDevicesFuture, totalDevicesFuture, totalReferralsFuture,
                totalConnectionsFuture, trafficSummaryFuture
        ).join();

        TrafficSummaryDto summary = trafficSummaryFuture.join();
        long bytesIn      = summary != null && summary.getBytesIn()     != null ? summary.getBytesIn()     : 0L;
        long bytesOut     = summary != null && summary.getBytesOut()    != null ? summary.getBytesOut()    : 0L;
        long spentKopecks = summary != null && summary.getCostKopecks() != null ? summary.getCostKopecks() : 0L;

        return UserStatsResponse.builder()
                .telegramId(telegramId)
                .balance(user.getBalance())
                .totalDevices(totalDevicesFuture.join().intValue())
                .activeDevices(activeDevicesFuture.join().intValue())
                .totalReferrals(totalReferralsFuture.join())
                .totalReferralEarnings((int) (totalReferralsFuture.join() * 50))
                .totalConnections(totalConnectionsFuture.join())
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

        CompletableFuture<TrafficSummaryDto> summaryFuture = CompletableFuture.supplyAsync(
                () -> trafficServiceClient.getTrafficSummary(telegramId).getData(), executorService);

        CompletableFuture<List<TrafficSessionDto>> sessionsFuture = CompletableFuture.supplyAsync(
                () -> trafficServiceClient.getRecentSessions(telegramId).getData(), executorService);

        CompletableFuture.allOf(summaryFuture, sessionsFuture).join();

        TrafficSummaryDto summary              = summaryFuture.join();
        List<TrafficSessionDto> recentSessions = sessionsFuture.join();

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

    @Override
    public UserResponse purchaseSubscription(Long telegramId, String planName, int months) {
        return purchaseSubscription(telegramId, planName, months, false);
    }

    @Override
    @Transactional(isolation = Isolation.REPEATABLE_READ)
    @CacheEvict(value = {"users", "user-max-devices"}, key = "#telegramId")
    public UserResponse purchaseSubscription(Long telegramId, String planName, int months, boolean promo) {
        log.info("purchaseSubscription: user={}, plan={}, months={}, promo={}", telegramId, planName, months, promo);

        User user = userRepository.findByTelegramId(telegramId)
                .orElseThrow(() -> new UserNotFoundException(telegramId));

        if (promo && "BASIC".equalsIgnoreCase(planName)) {
            if (user.getSubscriptionType() != SubscriptionType.PAYG) {
                throw new RuntimeException("Бесплатный период доступен только для новых пользователей без подписки.");
            }
            log.info("Granting FREE promo BASIC month to user {}", telegramId);

            user.setSubscriptionType(SubscriptionType.BASIC);
            user.setSubscriptionExpiresAt(LocalDateTime.now().plusDays(30));
            user.setPromoApplied(true);

            User savedUser = userRepository.saveAndFlush(user);
            syncVpnLimits(telegramId, "BASIC", savedUser);
            return userMapper.toResponse(savedUser);
        }

        int newPlanCost = getPlanCost(planName);
        double baseDays = "DAILY".equalsIgnoreCase(planName) ? 1.0 : 30.0;
        int purchasePrice = newPlanCost * months;

        if (!user.hasSufficientBalance(purchasePrice)) {
            throw new InsufficientBalanceException(purchasePrice, user.getBalance());
        }

        LocalDateTime now = LocalDateTime.now();
        double remainingMoney = 0;

        if (user.getSubscriptionExpiresAt() != null && user.getSubscriptionExpiresAt().isAfter(now)) {
            double oldBaseDays = "DAILY".equalsIgnoreCase(user.getSubscriptionType().name()) ? 1.0 : 30.0;
            double currentDailyPrice = getPlanCost(user.getSubscriptionType().name()) / oldBaseDays;

            long remainingDays = java.time.Duration.between(now, user.getSubscriptionExpiresAt()).toDays();
            remainingMoney = remainingDays * currentDailyPrice;
        }

        double totalBudget = remainingMoney + purchasePrice;
        double newDailyPrice = newPlanCost / baseDays;
        long totalDaysAvailable = (long) (totalBudget / newDailyPrice);

        long minDays = (long) (baseDays * months);
        if (totalDaysAvailable < minDays) {
            totalDaysAvailable = minDays;
        }

        user.deductBalance(purchasePrice);
        SubscriptionType type = SubscriptionType.valueOf(planName.toUpperCase());
        user.setSubscriptionType(type);
        user.setSubscriptionExpiresAt(now.plusDays(totalDaysAvailable));

        User savedUser = userRepository.saveAndFlush(user);
        syncVpnLimits(telegramId, planName, savedUser);

        return userMapper.toResponse(savedUser);
    }

    @Override
    @Transactional
    public boolean isUserMemberOfChannel(Long telegramId) {
        User user = userRepository.findByTelegramId(telegramId)
                .orElseThrow(() -> new UserNotFoundException(telegramId));

        if (user.getIsChannelMember() != null) {
            return user.getIsChannelMember();
        }

        boolean isMember = fetchMembershipFromTelegramApi(telegramId);

        user.setIsChannelMember(isMember);
        userRepository.save(user);

        return isMember;
    }

    @Override
    @Transactional
    @CacheEvict(value = "users", key = "#telegramId")
    public void updateMembershipStatus(Long telegramId, boolean isMember) {
        userRepository.findByTelegramId(telegramId).ifPresent(user -> {
            user.setIsChannelMember(isMember);
            userRepository.save(user);
            log.info("Membership status updated via bot event for user {}: {}", telegramId, isMember);
        });
    }

    private boolean fetchMembershipFromTelegramApi(Long telegramId) {
        String url = "https://api.telegram.org/bot" + botToken + "/getChatMember?chat_id=" + channelId + "&user_id=" + telegramId;

        try {
            log.info("Checking TG membership via restTemplate for the first time: user={}", telegramId);
            JsonNode response = restTemplate.getForObject(url, JsonNode.class);

            if (response != null && response.get("ok").asBoolean()) {
                String status = response.get("result").get("status").asText();
                return List.of("member", "administrator", "creator").contains(status);
            }
        } catch (Exception e) {
            log.error("Error checking TG membership via restTemplate (falling back to true due to timeout): {}", e.getMessage());
            return true;
        }
        return false;
    }

    @Override
    @Transactional
    @CacheEvict(value = "users", key = "#telegramId")
    public UserResponse updateBanStatus(Long telegramId, boolean isBanned, String reason) {
        log.info("Updating ban status in DB for user {}: isBanned={}, reason={}", telegramId, isBanned, reason);

        User user = userRepository.findByTelegramId(telegramId)
                .orElseThrow(() -> new UserNotFoundException(telegramId));

        user.setBanned(isBanned);

        user.setBanReason(isBanned ? reason : null);

        User savedUser = userRepository.save(user);
        log.info("Ban status successfully updated in DB for user {}", telegramId);

        return userMapper.toResponse(savedUser);
    }

    @Override
    public UserInitResponse getUserInitData(Long telegramId) {
        log.info("Collecting aggregated init data for user: {}", telegramId);

        CompletableFuture<UserResponse> userFuture = CompletableFuture.supplyAsync(
                () -> self.getUserByTelegramId(telegramId), executorService);

        CompletableFuture<Boolean> memberFuture = CompletableFuture.supplyAsync(
                () -> self.isUserMemberOfChannel(telegramId), executorService);

        return CompletableFuture.allOf(userFuture, memberFuture)
                .thenApply(v -> UserInitResponse.builder()
                        .user(userFuture.join())
                        .isMember(memberFuture.join())
                        .build())
                .join();
    }

    @Override
    @Transactional(isolation = Isolation.REPEATABLE_READ)
    @CacheEvict(value = "user-max-devices", key = "#telegramId")
    public UserResponse purchaseExtraSlot(Long telegramId) {
        log.info("User {} purchasing extra device slot", telegramId);

        User user = userRepository.findByTelegramId(telegramId)
                .orElseThrow(() -> new UserNotFoundException(telegramId));

        int slotPrice = 10000;

        if (!user.hasSufficientBalance(slotPrice)) {
            throw new InsufficientBalanceException(slotPrice, user.getBalance());
        }

        user.deductBalance(slotPrice);
        User savedUser = userRepository.save(user);

        try {
            vpnServiceClient.addExtraDeviceSlot(telegramId);
        } catch (Exception e) {
            log.error("Failed to add extra slot in VPN service for user {}", telegramId, e);
            throw new RuntimeException("Ошибка синхронизации лимитов. Попробуйте позже.");
        }

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
            case "DAILY"     -> 600;
            case "BASIC"     -> 10000;
            case "STANDARD"  -> 15000;
            case "FAMILY"    -> 35000;
            default          -> 0;
        };
    }

    private int getMaxDevicesForPlan(String planName) {
        return switch (planName.toUpperCase()) {
            case "DAILY", "BASIC" -> 1;
            case "STANDARD"  -> 2;
            case "FAMILY"    -> 3;
            default          -> 1;
        };
    }

    @jakarta.annotation.PreDestroy
    public void shutdownExecutor() {
        log.info("Shutting down UserServiceImpl virtual threads executor...");
        executorService.shutdown();
    }
}