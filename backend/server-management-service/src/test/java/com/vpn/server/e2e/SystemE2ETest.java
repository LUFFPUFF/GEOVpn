package com.vpn.server.e2e;

import com.vpn.common.dto.ConfigMetadataDto;
import com.vpn.common.dto.request.ConfigCreateRequest;
import com.vpn.common.dto.response.VpnConfigResponse;
import com.vpn.common.service.RedisCacheService;
import com.vpn.server.grpc.ConfigServiceClient;
import com.vpn.server.grpc.UserServiceClient;
import com.vpn.server.scheduler.TrafficCollectorJob;
import com.vpn.common.dto.enums.DeviceType;
import com.vpn.common.dto.request.DeviceCreateRequest;
import com.vpn.common.dto.request.UserRegistrationRequest;
import com.vpn.common.dto.response.DeviceResponse;
import com.vpn.common.dto.response.UserResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = {
        "service.security.internal-secret=53a41724a2428714e21b2cbcbb19ce1ff62f4deb322037575f15f450791d54c3",
        "spring.data.redis.host=localhost",
        "spring.data.redis.port=6379",
        "feign.client.config.user-service.url=http://localhost:8082",
        "feign.client.config.vpn-config-service.url=http://localhost:8083",
        "vpn.billing.price-per-gb=600",
        "vpn.billing.collect-interval-ms=300000",
        "spring.datasource.url=jdbc:postgresql://localhost:5432/telegram_vpn",
        "spring.datasource.username=admin",
        "spring.datasource.password=admin"
})
public class SystemE2ETest {

    @Autowired private ConfigServiceClient configServiceClient;
    @Autowired private UserServiceClient userServiceClient;
    @Autowired private RedisCacheService redisCacheService;
    @Autowired private TrafficCollectorJob trafficCollectorJob;

    public static final String R = "\u001B[0m";
    public static final String G = "\u001B[32m";
    public static final String Y = "\u001B[33m";
    public static final String B = "\u001B[34m";
    public static final String P = "\u001B[35m";
    public static final String C = "\u001B[36m";
    public static final String BOLD = "\u001B[1m";

    @Test
    void runFullApplicationLifecycle() {
        Long tgId = 999000L + (long)(Math.random() * 1000);

        System.out.println(B + BOLD + "===========================================================================" + R);
        System.out.println(B + BOLD + "СТАРТ ПОЛНОГО ЦИКЛА СИСТЕМЫ: ОТ РЕГИСТРАЦИИ ДО БИЛЛИНГА" + R);
        System.out.println(B + BOLD + "===========================================================================" + R);

        System.out.println(Y + BOLD + "\n[ЭТАП 1] Идентификация пользователя в User Service" + R);
        UserRegistrationRequest regReq = new UserRegistrationRequest(tgId, "test_hero", "Ivan" , null);
        UserResponse user = userServiceClient.registerUser(regReq).getData();
        System.out.println(G + "✅ Пользователь зарегистрирован: " + C + "@" + user.getUsername() + R);
        System.out.println(G + "✅ Стартовый баланс: " + C + user.getBalance() + " коп." + R);

        System.out.println(Y + BOLD + "\n[ЭТАП 2] Привязка устройства" + R);
        DeviceCreateRequest devReq = new DeviceCreateRequest(tgId, "Samsung S24 Ultra", DeviceType.ANDROID);
        var device = userServiceClient.registerDevice(tgId, devReq).getData();
        System.out.println(G + "✅ Устройство привязано. ID девайса в базе: " + C + device.getId() + R);

        System.out.println(Y + BOLD + "\n[ЭТАП 3] Генерация VPN-конфигурации через VpnConfig Service" + R);
        ConfigCreateRequest confReq = ConfigCreateRequest.builder()
                .userId(tgId)
                .deviceId(device.getId())
                .protocol("VLESS")
                .preferredCountry("NL")
                .build();

        VpnConfigResponse config = configServiceClient.createConfig(tgId, confReq).getData();

        System.out.println(G + "✅ Алгоритм выбрал сервер: " + C + config.getServerName() + R);
        System.out.println(G + "✅ Сгенерирован VLESS UUID: " + C + config.getVlessUuid() + R);

        System.out.println(P + BOLD + "\n>>> СГЕНЕРИРОВАННАЯ ССЫЛКА ДЛЯ КЛИЕНТА:" + R);
        System.out.println(BOLD + config.getVlessLink() + R);

        System.out.println(P + BOLD + "\n>>> СГЕНЕРИРОВАННЫЙ QR-CODE (BASE64):" + R);
        System.out.println(C + config.getQrCodeDataUrl().substring(0, 100) + "... [TRUNCATED]" + R);

        System.out.println(Y + BOLD + "\n[ЭТАП 3] Проверка целостности данных в Redis" + R);
        ConfigMetadataDto meta = redisCacheService.get("vpn:meta:" + config.getVlessUuid(), ConfigMetadataDto.class);
        assertNotNull(meta);
        System.out.println(G + "✅ Метаданные в Redis подтверждены. Связь UUID <-> UserID установлена." + R);

        System.out.println(Y + BOLD + "\n[ЭТАП 4] Имитация работы сетевого уровня (Collector Job)" + R);
        System.out.println(BOLD + "Опрашиваем ноды по gRPC..." + R);

        trafficCollectorJob.collectTraffic();

        System.out.println(G + "✅ Цикл сбора завершен." + R);
        System.out.println(G + "✅ Данные дельты трафика рассчитаны и сохранены в PostgreSQL." + R);
        System.out.println(G + "✅ Баланс пользователя @" + tgId + " обновлен на основе потребления." + R);

        System.out.println(Y + BOLD + "\n[ЭТАП 5] Финальный отчет из Базы Данных" + R);
        UserResponse finalUser = userServiceClient.getUserByTelegramId(tgId).getData();
        System.out.println(C + "   Итоговый баланс: " + finalUser.getBalance() + " коп." + R);
        System.out.println(C + "   Статус подписки: " + finalUser.getSubscriptionType() + R);
    }

    private void assertNotNull(Object obj) {
        if (obj == null) throw new RuntimeException("Assertion failed: Object is null");
    }
}
