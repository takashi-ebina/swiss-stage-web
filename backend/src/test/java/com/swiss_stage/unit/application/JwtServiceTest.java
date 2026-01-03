package com.swiss_stage.unit.application;

import com.swiss_stage.application.service.JwtService;
import com.swiss_stage.domain.model.AuthSession;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * JwtServiceのユニットテスト
 * TDD: Red-Green-Refactor
 */
class JwtServiceTest {

    private JwtService jwtService;
    private final String secretKey = "test-secret-key-for-jwt-signature-must-be-at-least-256-bits-long";
    private final int expirationHours = 24;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService(secretKey, expirationHours);
    }

    @Test
    void generateToken_正常系_有効なJWTトークンを生成できる() {
        // Arrange
        UUID userId = UUID.randomUUID();

        // Act
        String token = jwtService.generateToken(userId);

        // Assert
        assertNotNull(token);
        assertFalse(token.isEmpty());
        assertTrue(token.split("\\.").length == 3); // JWT format: header.payload.signature
    }

    @Test
    void validateTokenAndGetUserId_正常系_有効なトークンからuserIdを取得できる() {
        // Arrange
        UUID userId = UUID.randomUUID();
        String token = jwtService.generateToken(userId);

        // Act
        UUID extractedUserId = jwtService.validateTokenAndGetUserId(token);

        // Assert
        assertNotNull(extractedUserId);
        assertEquals(userId, extractedUserId);
    }

    @Test
    void validateTokenAndGetUserId_異常系_不正なトークンで例外をスローする() {
        // Arrange
        String invalidToken = "invalid.jwt.token";

        // Act & Assert
        assertThrows(RuntimeException.class, () -> {
            jwtService.validateTokenAndGetUserId(invalidToken);
        });
    }

    @Test
    void validateTokenAndGetUserId_異常系_署名が不正なトークンで例外をスローする() {
        // Arrange
        UUID userId = UUID.randomUUID();
        String token = jwtService.generateToken(userId);
        
        // トークンの最後の文字を変更して署名を破壊
        String tamperedToken = token.substring(0, token.length() - 1) + "X";

        // Act & Assert
        assertThrows(RuntimeException.class, () -> {
            jwtService.validateTokenAndGetUserId(tamperedToken);
        });
    }

    @Test
    void createAuthSession_正常系_認証セッションを作成できる() {
        // Arrange
        UUID userId = UUID.randomUUID();
        Duration validity = Duration.ofHours(24);

        // Act
        AuthSession session = jwtService.createAuthSession(userId, validity);

        // Assert
        assertNotNull(session);
        assertEquals(userId, session.userId());
        assertNotNull(session.jwtToken());
        assertFalse(session.isExpired());
        assertTrue(session.isValid());
    }
}
