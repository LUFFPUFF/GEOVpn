package com.vpn.gateway.util;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Base64;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;


/**
 * Утилита для работы с JWT токенами
 * Использует HS512 для подписи
 */

@Slf4j
@Component
public class JwtUtil {

    @Value("${service.jwt.secret}")
    private String secretBase64;

    @Value("${service.jwt.expiration}")
    private long expirationTime;

    @Value("${service.jwt.refresh-expiration}")
    private long refreshExpirationTime;

    private SecretKey secretKey;

    @PostConstruct
    public void init() {
        try {
            byte[] decodedKey = Base64.getDecoder().decode(secretBase64);
            this.secretKey = Keys.hmacShaKeyFor(decodedKey);
            log.info("JWT Secret Key initialized successfully");
        } catch (Exception e) {
            log.error("Failed to initialize JWT secret key", e);
            throw new IllegalStateException("Invalid JWT secret key", e);
        }
    }

    /**
     * Извлечь все Claims из токена
     */
    private Claims getAllClaimsFromToken(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(secretKey)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    /**
     * Получить User ID (Telegram ID) из токена
     */
    public String getUserIdFromToken(String token) {
        return getAllClaimsFromToken(token).getSubject();
    }

    /**
     * Получить custom claim из токена
     */
    public String getClaimFromToken(String token, String claimName) {
        return getAllClaimsFromToken(token).get(claimName, String.class);
    }

    /**
     * Проверить истек ли токен
     */
    private boolean isTokenExpired(String token) {
        try {
            Date expiration = getAllClaimsFromToken(token).getExpiration();
            return expiration.before(new Date());
        } catch (ExpiredJwtException e) {
            return true;
        }
    }

    /**
     * Валидация токена
     */
    public boolean validateToken(String token) {
        try {
            Jwts.parserBuilder()
                    .setSigningKey(secretKey)
                    .build()
                    .parseClaimsJws(token);

            return !isTokenExpired(token);
        } catch (ExpiredJwtException e) {
            log.debug("Token expired: {}", e.getMessage());
            return false;
        } catch (UnsupportedJwtException e) {
            log.warn("Unsupported JWT token: {}", e.getMessage());
            return false;
        } catch (MalformedJwtException e) {
            log.warn("Malformed JWT token: {}", e.getMessage());
            return false;
        } catch (SignatureException e) {
            log.warn("Invalid JWT signature: {}", e.getMessage());
            return false;
        } catch (IllegalArgumentException e) {
            log.warn("JWT token compact of handler are invalid: {}", e.getMessage());
            return false;
        } catch (Exception e) {
            log.error("JWT validation error", e);
            return false;
        }
    }

    /**
     * Генерация Access Token
     */
    public String generateToken(String telegramId) {
        return generateToken(telegramId, new HashMap<>());
    }

    /**
     * Генерация Access Token с дополнительными claims
     */
    public String generateToken(String telegramId, Map<String, Object> additionalClaims) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + expirationTime);

        JwtBuilder builder = Jwts.builder()
                .setSubject(telegramId)
                .setIssuedAt(now)
                .setExpiration(expiryDate)
                .signWith(secretKey, SignatureAlgorithm.HS512);

        additionalClaims.forEach(builder::claim);

        return builder.compact();
    }

    /**
     * Генерация Refresh Token
     */
    public String generateRefreshToken(String telegramId) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + refreshExpirationTime);

        return Jwts.builder()
                .setSubject(telegramId)
                .setIssuedAt(now)
                .setExpiration(expiryDate)
                .claim("type", "refresh")
                .signWith(secretKey, SignatureAlgorithm.HS512)
                .compact();
    }

    /**
     * Получить время до истечения токена (в миллисекундах)
     */
    public long getExpirationTime(String token) {
        Date expiration = getAllClaimsFromToken(token).getExpiration();
        return expiration.getTime() - System.currentTimeMillis();
    }
}
