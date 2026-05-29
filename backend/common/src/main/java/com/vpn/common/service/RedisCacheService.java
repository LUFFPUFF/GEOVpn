package com.vpn.common.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class RedisCacheService {

    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper;

    /**
     * Сохранить значение с TTL
     */
    public void set(String key, Object value, Duration ttl) {
        try {
            redisTemplate.opsForValue().set(key, value, ttl);
            log.debug("Cached: {} with TTL: {}", key, ttl);
        } catch (Exception e) {
            log.error("Failed to cache key: {}", key, e);
        }
    }

    /**
     * Получить значение
     */
    public <T> T get(String key, Class<T> type) {
        try {
            Object value = redisTemplate.opsForValue().get(key);
            if (value == null) return null;

            if (type.isInstance(value)) {
                return type.cast(value);
            }

            return objectMapper.convertValue(value, type);
        } catch (Exception e) {
            log.error("Failed to convert key: {}", key, e);
            return null;
        }
    }

    /**
     * Удалить ключ
     */
    public void delete(String key) {
        try {
            redisTemplate.delete(key);
            log.debug("Deleted cache key: {}", key);
        } catch (Exception e) {
            log.error("Failed to delete key: {}", key, e);
        }
    }

    public void deletePattern(String pattern) {
        try {
            ScanOptions options = ScanOptions.scanOptions().match(pattern).count(100).build();

            redisTemplate.execute((RedisCallback<Void>) connection -> {
                try (Cursor<byte[]> cursor = connection.keyCommands().scan(options)) {
                    List<byte[]> keysToDel = new ArrayList<>();
                    while (cursor.hasNext()) {
                        keysToDel.add(cursor.next());

                        if (keysToDel.size() >= 100) {
                            connection.keyCommands().del(keysToDel.toArray(new byte[0][]));
                            keysToDel.clear();
                        }
                    }
                    if (!keysToDel.isEmpty()) {
                        connection.keyCommands().del(keysToDel.toArray(new byte[0][]));
                    }
                } catch (Exception e) {
                    log.error("Error scanning keys with pattern: {}", pattern, e);
                }
                return null;
            });

            log.info("Successfully deleted keys matching pattern cleanly: {}", pattern);
        } catch (Exception e) {
            log.error("Failed to delete pattern: {}", pattern, e);
        }
    }

    /**
     * Проверить существование ключа
     */
    public boolean exists(String key) {
        try {
            return redisTemplate.hasKey(key);
        } catch (Exception e) {
            log.error("Failed to check key existence: {}", key, e);
            return false;
        }
    }

    /**
     * Инкремент (для счетчиков)
     */
    public Long increment(String key) {
        try {
            return redisTemplate.opsForValue().increment(key);
        } catch (Exception e) {
            log.error("Failed to increment key: {}", key, e);
            return null;
        }
    }

    /**
     * Работа с Hash структурами
     */
    public void hSet(String key, String field, Object value) {
        try {
            redisTemplate.opsForHash().put(key, field, value);
            log.debug("Hash set: {}:{}", key, field);
        } catch (Exception e) {
            log.error("Failed to set hash: {}:{}", key, field, e);
        }
    }

    public <T> T hGet(String key, String field, Class<T> type) {
        try {
            Object value = redisTemplate.opsForHash().get(key, field);
            if (type.isInstance(value)) {
                return type.cast(value);
            }
            return null;
        } catch (Exception e) {
            log.error("Failed to get hash: {}:{}", key, field, e);
            return null;
        }
    }

    /**
     * Установить TTL для существующего ключа
     */
    public boolean expire(String key, Duration ttl) {
        try {
            Boolean result = redisTemplate.expire(key, ttl);
            return result != null && result;
        } catch (Exception e) {
            log.error("Failed to set expiry for key: {}", key, e);
            return false;
        }
    }
}