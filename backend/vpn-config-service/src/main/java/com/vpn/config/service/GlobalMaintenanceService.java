package com.vpn.config.service;

import com.vpn.common.dto.ServerDto;
import com.vpn.common.dto.response.DeviceResponse;
import com.vpn.common.dto.response.UserResponse;
import com.vpn.common.service.RedisCacheService;
import com.vpn.config.client.ServerManagementClient;
import com.vpn.config.client.UserServiceClient;
import com.vpn.config.client.XUIServerApiClient;
import com.vpn.config.domain.entity.VpnConfiguration;
import com.vpn.common.dto.enums.ConfigStatus;
import com.vpn.config.repository.VpnConfigurationRepository;
import com.vpn.config.service.interf.ServerSelectionService;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
@Service
@RequiredArgsConstructor
public class GlobalMaintenanceService {

    /** Размер батча при переборе пользователей (запросы к User-Service). */
    private static final int BATCH_SIZE = 200;

    /**
     * Максимальное количество ПАРАЛЛЕЛЬНЫХ HTTP-запросов к одной XUI-панели.
     * SQLite не выдерживает больше ~10 одновременных write-запросов:
     * при превышении этого числа начинаются "database is locked" → таймауты.
     *
     * Значение выбрано с запасом безопасности: реальный предел ≈ 15,
     * мы держим 10, чтобы оставить ресурс для собственного Xray-трафика.
     */
    private static final int XUI_CONCURRENCY_PER_SERVER = 10;

    /** Время жизни SSE-соединения с фронтендом (15 минут достаточно для полного прогона). */
    private static final long SSE_TIMEOUT_MS = Duration.ofMinutes(20).toMillis();

    /** Сердцебиение SSE: раз в 25 секунд, чтобы nginx/браузер не закрыл idle-соединение. */
    private static final long SSE_HEARTBEAT_INTERVAL_MS = 25_000L;

    private static final List<Integer> TOKEN_SERVER_IDS     = List.of(8, 4, 3);
    private static final List<String>  TOKEN_SERVER_TOKENS  = List.of("UPDATE_ME_8", "UPDATE_ME_4", "UPDATE_ME_3");


    private final UserServiceClient            userServiceClient;
    private final ServerManagementClient       serverManagementClient;
    private final VpnConfigurationRepository   configRepository;
    private final ServerSelectionService       serverSelectionService;
    private final XUIServerApiClient           xuiClient;
    private final VpnLinksBuilder              vpnLinksBuilder;
    private final RedisCacheService            redisCacheService;

    /**
     * Пул виртуальных потоков для параллельных задач.
     * Virtual threads — лёгкие, создаём сколько нужно,
     * но реальный параллелизм к XUI ограничиваем через Semaphore ниже.
     */
    private final ExecutorService virtualExecutor = Executors.newVirtualThreadPerTaskExecutor();


    /** Живые SSE-соединения с фронтом (CopyOnWriteArrayList — lock-free на чтение). */
    private final List<SseEmitter> emitters = new CopyOnWriteArrayList<>();

    /**
     * Планировщик heartbeat-пингов.
     * Единственный поток достаточен: задача — отправить мелкий JSON раз в 25 сек.
     */
    private final ScheduledExecutorService heartbeatScheduler =
            Executors.newSingleThreadScheduledExecutor(r -> {
                Thread t = new Thread(r, "sse-heartbeat");
                t.setDaemon(true);
                return t;
            });

    /**
     * Статический инициализатор heartbeat: запускается один раз при создании бина.
     * Spring вызывает конструктор → поле инициализировано → @PostConstruct не нужен,
     * но здесь удобнее использовать init-блок.
     */
    {
        heartbeatScheduler.scheduleAtFixedRate(
                this::sendHeartbeat,
                SSE_HEARTBEAT_INTERVAL_MS,
                SSE_HEARTBEAT_INTERVAL_MS,
                TimeUnit.MILLISECONDS
        );
    }

    /**
     * Регистрирует нового SSE-подписчика.
     *
     * <p>Особенности:
     * <ul>
     *   <li>Таймаут 20 мин — с запасом относительно реального прогона.</li>
     *   <li>onCompletion/onTimeout/onError — чистим список эмиттеров немедленно.</li>
     *   <li>Сразу отправляем "connected"-пинг, чтобы клиент убедился в живом стриме.</li>
     * </ul>
     */
    public SseEmitter registerEmitter() {
        SseEmitter emitter = new SseEmitter(SSE_TIMEOUT_MS);

        Runnable cleanup = () -> emitters.remove(emitter);
        emitter.onCompletion(cleanup);
        emitter.onTimeout(cleanup);
        emitter.onError(e -> {
            log.debug("SSE emitter error (client likely disconnected): {}", e.getMessage());
            cleanup.run();
        });

        emitters.add(emitter);

        try {
            emitter.send(SseEmitter.event()
                    .name("connected")
                    .data(Map.of(
                            "timestamp", LocalDateTime.now().toString(),
                            "message",   "SSE stream established. Waiting for maintenance events..."
                    )));
        } catch (Exception e) {
            log.warn("Could not send initial SSE ping: {}", e.getMessage());
            emitters.remove(emitter);
        }

        log.info("SSE emitter registered. Total active emitters: {}", emitters.size());
        return emitter;
    }

    /**
     * Точка входа: полный прогон технического обслуживания.
     * Вызывается из {@code AdminController} в фоновом CompletableFuture.
     */
    public MaintenanceReport runFullMaintenance() {
        long startMs = System.currentTimeMillis();
        broadcastLog("=== GLOBAL MAINTENANCE STARTED ===", "WARN");

        MaintenanceReport report = new MaintenanceReport();

        List<VpnConfiguration> activeConfigs = configRepository.findByStatus(ConfigStatus.ACTIVE);
        List<String> activeEmails = activeConfigs.stream()
                .map(c -> "tg_" + c.getUserId() + "_dev_" + c.getDeviceId())
                .distinct()
                .toList();

        broadcastLog("[STEP 2] Found " + activeEmails.size()
                + " active configurations in local DB to clear from XUI panels", "INFO");

        step3ClearXuiPanels(activeEmails, report);

        step4ClearDatabase();

        List<Long> targetUserIds = Collections.emptyList();
        try {
            var response = userServiceClient.getAllActiveUserIds();
            if (response != null && response.getData() != null) {
                targetUserIds = response.getData();
            }
        } catch (Exception e) {
            broadcastLog("[STEP 5] Failed to fetch active users from User-Service: " + e.getMessage(), "ERROR");
        }

        broadcastLog("[STEP 5] Found " + targetUserIds.size()
                + " active users from User-Service to re-sync", "INFO");

        step5BatchGenerateAndSync(targetUserIds, report);

        long elapsedSec = (System.currentTimeMillis() - startMs) / 1000;
        report.setElapsedSeconds(elapsedSec);

        broadcastLog("=== GLOBAL MAINTENANCE COMPLETED in " + elapsedSec + "s"
                + " | processed=" + report.getUsersProcessed()
                + " | failed="    + report.getUsersFailed() + " ===", "WARN");

        broadcastReport(report);
        return report;
    }

    protected void step1UpdateServerTokens() {
        broadcastLog("[STEP 1] Updating API tokens for servers: " + TOKEN_SERVER_IDS, "INFO");
        for (int i = 0; i < TOKEN_SERVER_IDS.size(); i++) {
            Integer serverId = TOKEN_SERVER_IDS.get(i);
            String  token    = TOKEN_SERVER_TOKENS.get(i);
            try {
                serverManagementClient.updateServer(serverId, Map.of("apiToken", token));
                broadcastLog("Token updated for server id=" + serverId, "INFO");
            } catch (Exception e) {
                broadcastLog("Failed to update token for server id=" + serverId + ": " + e.getMessage(), "ERROR");
            }
        }
    }

    /**
     * Шаг 3: параллельно очищаем клиентов на всех XUI-серверах.
     *
     * <p><b>Ключевое изменение</b>: для каждого сервера создаётся свой {@link Semaphore}
     * на {@value XUI_CONCURRENCY_PER_SERVER} разрешений. Это гарантирует,
     * что к одной SQLite-базе панели одновременно пишет не более 10 потоков,
     * что полностью исключает "database is locked" и перегрузку CPU Xray-перезапусками.
     */
    protected void step3ClearXuiPanels(List<String> activeEmails, MaintenanceReport report) {
        if (activeEmails.isEmpty()) {
            broadcastLog("[STEP 3] No active configurations found. Skipping XUI panels clear.", "WARN");
            return;
        }

        List<ServerDto> allServers = serverSelectionService.getAllActiveServers();
        broadcastLog("[STEP 3] Clearing " + activeEmails.size() + " clients on "
                + allServers.size() + " servers (max " + XUI_CONCURRENCY_PER_SERVER
                + " concurrent requests per server)...", "INFO");

        List<CompletableFuture<Void>> serverFutures = allServers.stream()
                .map(server -> CompletableFuture.runAsync(
                        () -> clearSingleServer(server, activeEmails, report),
                        virtualExecutor))
                .toList();

        awaitAll(serverFutures);
        broadcastLog("[STEP 3] XUI panels clearing completed.", "INFO");
    }

    /**
     * Удаляем всех переданных клиентов с одного сервера,
     * ограничивая конкурентность через {@link Semaphore}.
     */
    private void clearSingleServer(ServerDto server, List<String> emails, MaintenanceReport report) {
        Semaphore semaphore = new Semaphore(XUI_CONCURRENCY_PER_SERVER);

        broadcastLog("Clearing " + emails.size() + " clients from server=" + server.getName()
                + " (concurrency=" + XUI_CONCURRENCY_PER_SERVER + ")...", "INFO");

        List<CompletableFuture<Void>> deleteFutures = emails.stream()
                .map(email -> CompletableFuture.runAsync(() -> {
                    try {
                        semaphore.acquire();                   // ← ждём разрешения
                        try {
                            xuiClient.deleteClientByEmail(server, email);
                        } finally {
                            semaphore.release();               // ← всегда освобождаем
                        }
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        log.warn("Interrupted while waiting for semaphore on server={}", server.getName());
                    } catch (Exception e) {
                        // Отдельные ошибки удаления логируем тихо, не прерывая весь прогон
                        log.debug("Failed to delete email={} on server={}: {}", email, server.getName(), e.getMessage());
                    }
                }, virtualExecutor))
                .toList();

        awaitAll(deleteFutures);
        report.incrementServersCleared();
        broadcastLog("Done clearing server=" + server.getName(), "INFO");
    }

    protected void step4ClearDatabase() {
        broadcastLog("[STEP 4] Bulk-deleting all VPN configurations from DB...", "INFO");
        int deleted = configRepository.deleteAllConfigsFast();
        broadcastLog("Deleted " + deleted + " records from vpn_configurations.", "INFO");

        try {
            redisCacheService.deletePattern("vpn:meta:*");
            redisCacheService.deletePattern("vpn-configs*");
            broadcastLog("Redis VPN caches flushed.", "INFO");
        } catch (Exception e) {
            broadcastLog("Failed to flush Redis caches: " + e.getMessage(), "WARN");
        }
    }

    protected void step5BatchGenerateAndSync(List<Long> targetUserIds, MaintenanceReport report) {
        broadcastLog("[STEP 5] Starting batch generation & sync for "
                + targetUserIds.size() + " users...", "INFO");

        List<ServerDto> allServers = serverSelectionService.getAllActiveServers();
        AtomicInteger processed = new AtomicInteger(0);
        AtomicInteger failed    = new AtomicInteger(0);

        int totalBatches = (int) Math.ceil((double) targetUserIds.size() / BATCH_SIZE);

        for (int i = 0; i < targetUserIds.size(); i += BATCH_SIZE) {
            List<Long> batchIds = targetUserIds.subList(i, Math.min(i + BATCH_SIZE, targetUserIds.size()));
            int batchNum = (i / BATCH_SIZE) + 1;
            broadcastLog("[STEP 5] Processing batch " + batchNum + "/" + totalBatches
                    + " (" + batchIds.size() + " users)...", "INFO");
            processBatch(batchIds, allServers, processed, failed);
        }

        report.setUsersProcessed(processed.get());
        report.setUsersFailed(failed.get());
    }

    private void processBatch(List<Long> batchIds, List<ServerDto> allServers,
                              AtomicInteger processed, AtomicInteger failed) {

        List<VpnConfiguration> configsToSave   = new ArrayList<>();
        Map<VpnConfiguration, Long> expiryMap  = new HashMap<>();

        List<CompletableFuture<Void>> prepareFutures = batchIds.stream()
                .map(userId -> CompletableFuture.runAsync(() -> {
                    try {
                        UserResponse user = userServiceClient.getUserByTelegramId(userId).getData();
                        if (user == null || user.isBanned()) return;

                        LocalDateTime expiryDate = parseLocalDateTime(user.getSubscriptionExpiresAt());
                        if (expiryDate == null || expiryDate.isBefore(LocalDateTime.now())) return;

                        long expiryMs = expiryDate.toInstant(ZoneOffset.UTC).toEpochMilli();

                        List<DeviceResponse> devices = userServiceClient.getUserActiveDevices(userId).getData();
                        if (devices == null || devices.isEmpty()) return;

                        for (DeviceResponse device : devices) {
                            VpnConfiguration config = buildNewConfig(user, device, allServers);
                            synchronized (configsToSave) {
                                configsToSave.add(config);
                                expiryMap.put(config, expiryMs);
                            }
                        }
                    } catch (Exception e) {
                        broadcastLog("Failed to prepare config for userId=" + userId + ": " + e.getMessage(), "ERROR");
                        failed.incrementAndGet();
                    }
                }, virtualExecutor))
                .toList();

        awaitAll(prepareFutures);

        if (configsToSave.isEmpty()) return;

        List<VpnConfiguration> savedConfigs = configRepository.saveAll(configsToSave);

        Map<Integer, Semaphore> serverSemaphores = new ConcurrentHashMap<>();
        for (ServerDto s : allServers) {
            serverSemaphores.put(s.getId(), new Semaphore(XUI_CONCURRENCY_PER_SERVER));
        }

        List<CompletableFuture<Void>> xuiFutures = savedConfigs.stream()
                .map(config -> CompletableFuture.runAsync(() -> {
                    try {
                        long expiryMs = expiryMap.getOrDefault(config, 0L);
                        syncConfigToXuiThrottled(config, allServers, expiryMs, serverSemaphores);
                        processed.incrementAndGet();
                    } catch (Exception e) {
                        broadcastLog("XUI sync failed for uuid=" + config.getVlessUuid()
                                + ": " + e.getMessage(), "ERROR");
                        failed.incrementAndGet();
                    }
                }, virtualExecutor))
                .toList();

        awaitAll(xuiFutures);
    }

    /**
     * Синхронизирует один конфиг на все серверы, соблюдая per-server rate-limit.
     */
    private void syncConfigToXuiThrottled(VpnConfiguration config,
                                          List<ServerDto> allServers,
                                          long expiryTimeMillis,
                                          Map<Integer, Semaphore> serverSemaphores) {
        String email = "tg_" + config.getUserId() + "_dev_" + config.getDeviceId();
        String uuid  = config.getVlessUuid().toString();

        for (ServerDto server : allServers) {
            Semaphore sem = serverSemaphores.get(server.getId());
            try {
                if (sem != null) sem.acquire();
                try {
                    List<Integer> inboundIds = resolveInboundIds(server);
                    if (inboundIds.isEmpty()) continue;
                    xuiClient.addClientWithExpiryTime(server, inboundIds, uuid, email, "xtls-rprx-vision", expiryTimeMillis);
                } finally {
                    if (sem != null) sem.release();
                }
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
                log.warn("Interrupted during XUI sync for uuid={} server={}", uuid, server.getName());
            } catch (Exception e) {
                broadcastLog("Failed to sync uuid=" + uuid + " to server=" + server.getName()
                        + ": " + e.getMessage(), "WARN");
            }
        }
    }

    private VpnConfiguration buildNewConfig(UserResponse user, DeviceResponse device, List<ServerDto> allServers) {
        UUID vlessUuid = UUID.nameUUIDFromBytes(
                (user.getTelegramId() + "_" + device.getId()).getBytes());

        ServerDto primaryServer = allServers.stream()
                .filter(s -> !s.isRelay())
                .findFirst()
                .orElse(allServers.getFirst());

        String sni = primaryServer.getRealitySni() != null
                ? primaryServer.getRealitySni()
                : "www.microsoft.com";

        String primaryLink = String.format(
                "vless://%s@%s:%d?security=reality&encryption=none&pbk=%s&fp=chrome&sni=%s&sid=%s&type=tcp&flow=xtls-rprx-vision#GeoVPN",
                vlessUuid, primaryServer.getIpAddress(), primaryServer.getPort(),
                primaryServer.getRealityPublicKey(), sni, primaryServer.getRealityShortId());

        VpnConfiguration config = VpnConfiguration.builder()
                .vlessUuid(vlessUuid)
                .userId(user.getTelegramId())
                .deviceId(device.getId())
                .serverId(primaryServer.getId())
                .vlessLink(primaryLink)
                .deviceName(device.getDeviceName() != null ? device.getDeviceName() : "Unknown Device")
                .deviceOs(device.getDeviceType() != null ? device.getDeviceType().name() : "Unknown")
                .status(ConfigStatus.ACTIVE)
                .build();

        vpnLinksBuilder.buildAndStore(config);
        return config;
    }

    private List<Integer> resolveInboundIds(ServerDto server) {
        List<Integer> ids = new ArrayList<>();
        if (server.getTcpInboundId() != null) ids.add(server.getTcpInboundId());
        if (server.getWsInboundId()  != null) ids.add(server.getWsInboundId());
        if (ids.isEmpty() && server.getPanelInboundId() != null) ids.add(server.getPanelInboundId());
        return ids;
    }

    private LocalDateTime parseLocalDateTime(Object obj) {
        return switch (obj) {
            case null             -> null;
            case LocalDateTime ldt -> ldt;
            case String s         -> {
                try { yield LocalDateTime.parse(s); }
                catch (Exception e) {
                    broadcastLog("Failed to parse date: " + s, "WARN");
                    yield null;
                }
            }
            default -> null;
        };
    }

    private void awaitAll(List<CompletableFuture<Void>> futures) {
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
    }

    /**
     * Логирует сообщение в консоль бэкенда и транслирует всем подключённым клиентам.
     * Мёртвые эмиттеры (IOException при отправке) немедленно удаляются из списка.
     */
    private void broadcastLog(String message, String level) {
        switch (level) {
            case "INFO"  -> log.info(message);
            case "WARN"  -> log.warn(message);
            case "ERROR" -> log.error(message);
        }

        Map<String, String> payload = Map.of(
                "timestamp", LocalDateTime.now().toString(),
                "level",     level,
                "message",   message
        );

        broadcastEvent("log", payload);
    }

    /**
     * Отправляет финальный отчёт и закрывает SSE-соединения.
     */
    private void broadcastReport(MaintenanceReport report) {
        broadcastEvent("report", report);

        try { Thread.sleep(500); } catch (InterruptedException ignored) { Thread.currentThread().interrupt(); }

        List<SseEmitter> snapshot = new ArrayList<>(emitters);
        for (SseEmitter emitter : snapshot) {
            try { emitter.complete(); } catch (Exception ignored) {}
        }
        emitters.clear();
    }

    /**
     * Общий метод отправки SSE-события.
     * CopyOnWriteArrayList итерируется по snapshot-у — безопасно при одновременных удалениях.
     */
    private void broadcastEvent(String eventName, Object data) {
        List<SseEmitter> dead = new ArrayList<>();
        for (SseEmitter emitter : emitters) {
            try {
                emitter.send(SseEmitter.event().name(eventName).data(data));
            } catch (Exception e) {
                dead.add(emitter);
            }
        }
        if (!dead.isEmpty()) {
            emitters.removeAll(dead);
            log.debug("Removed {} dead SSE emitters", dead.size());
        }
    }

    /**
     * Периодический heartbeat, чтобы nginx/браузер не закрыл idle SSE-соединение.
     * Отправляем пустой "ping" каждые {@value SSE_HEARTBEAT_INTERVAL_MS} мс.
     */
    private void sendHeartbeat() {
        if (emitters.isEmpty()) return;
        broadcastEvent("ping", Map.of("ts", System.currentTimeMillis()));
    }


    @Getter
    public static class MaintenanceReport {
        @Setter private int  usersProcessed = 0;
        @Setter private int  usersFailed    = 0;
        private int          serversCleared = 0;
        @Setter private long elapsedSeconds = 0;

        public void incrementServersCleared() { this.serversCleared++; }
    }
}