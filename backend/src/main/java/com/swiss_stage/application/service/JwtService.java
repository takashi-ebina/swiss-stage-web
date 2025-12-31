package com.swiss_stage.application.service;

import com.swiss_stage.domain.model.AuthSession;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;

/**
 * JWTトークン生成・検証サービス
 * 憲章原則I「ドメイン駆動設計」に準拠（Application層）
 */
@Service
public class JwtService {

    private final SecretKey secretKey;
    private final Duration expiration;

    public JwtService(
            @Value("${jwt.secret-key}") String secretKeyString,
            @Value("${jwt.expiration-hours}") int expirationHours) {
        this.secretKey = Keys.hmacShaKeyFor(secretKeyString.getBytes(StandardCharsets.UTF_8));
        this.expiration = Duration.ofHours(expirationHours);
    }

    /**
     * JWTトークンを生成
     * @param userId ユーザーID
     * @return JWT token文字列
     */
    public String generateToken(UUID userId) {
        Instant now = Instant.now();
        Instant expirationTime = now.plus(expiration);

        return Jwts.builder()
                .subject(userId.toString())
                .issuedAt(Date.from(now))
                .expiration(Date.from(expirationTime))
                .signWith(secretKey)
                .compact();
    }

    /**
     * JWTトークンを検証し、userIdを取得
     * @param token JWTトークン
     * @return userId
     * @throws RuntimeException トークンが不正な場合
     */
    public UUID validateTokenAndGetUserId(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(secretKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            String userIdString = claims.getSubject();
            return UUID.fromString(userIdString);
        } catch (Exception e) {
            throw new RuntimeException("Invalid JWT token", e);
        }
    }

    /**
     * 認証セッションを作成
     * @param userId ユーザーID
     * @param validity 有効期限
     * @return AuthSession
     */
    public AuthSession createAuthSession(UUID userId, Duration validity) {
        String token = generateToken(userId);
        return AuthSession.create(token, userId, validity);
    }
}
