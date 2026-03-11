package com.vpn.server.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class TrafficCalculationService {

    private final StringRedisTemplate redisTemplate;

    private static final String REDIS_KEY_FORMAT = "traffic:last_raw:%d:%d:%s";

    public long calculateDelta(Integer serverId, Long configId, String type, long currentRawValue) {

        String key = String.format(REDIS_KEY_FORMAT, serverId, configId, type);

        String lastValueStr = redisTemplate.opsForValue().get(key);
        long delta;

        if (lastValueStr == null) {
            delta = currentRawValue;
            log.debug("First traffic record for config {}: {} bytes", configId, delta);
        } else {
            long lastRawValue = Long.parseLong(lastValueStr);

            if (currentRawValue < lastRawValue) {
                delta = currentRawValue;
                log.info("Server {} reboot detected for config {}. Counter reset.", serverId, configId);
            } else {
                delta = currentRawValue - lastRawValue;
            }
        }

        redisTemplate.opsForValue().set(key, String.valueOf(currentRawValue), 24, TimeUnit.HOURS);

        return delta;
    }

    public int calculateCost(long bytesTotal, double pricePerGb) {
        if (pricePerGb <= 0) return 0;

        double gigabytes = bytesTotal / (1024.0 * 1024.0 * 1024.0);
        return (int) Math.ceil(gigabytes * pricePerGb);
    }
}
