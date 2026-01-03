package com.swiss_stage.unit.domain;

import com.swiss_stage.domain.model.AuthSession;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * AuthSession値オブジェクトのドメインロジックをテスト
 * TDD: Red-Green-Refactor
 */
class AuthSessionTest {

    @Test
    void create_正常系_認証セッションを作成できる() {
        // Arrange
        String jwtToken = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...";
        UUID userId = UUID.randomUUID();
        Duration validity = Duration.ofHours(24);

        // Act
        AuthSession session = AuthSession.create(jwtToken, userId, validity);

        // Assert
        assertNotNull(session);
        assertEquals(jwtToken, session.jwtToken());
        assertEquals(userId, session.userId());
        assertNotNull(session.expiresAt());
        assertTrue(session.expiresAt().isAfter(Instant.now()));
    }

    @Test
    void isExpired_正常系_有効期限内の場合falseを返す() {
        // Arrange
        String jwtToken = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...";
        UUID userId = UUID.randomUUID();
        Duration validity = Duration.ofHours(24);
        AuthSession session = AuthSession.create(jwtToken, userId, validity);

        // Act & Assert
        assertFalse(session.isExpired());
    }

    @Test
    void isExpired_正常系_有効期限切れの場合trueを返す() {
        // Arrange
        String jwtToken = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...";
        UUID userId = UUID.randomUUID();
        Duration validity = Duration.ofSeconds(-1); // 過去の時刻
        AuthSession session = AuthSession.create(jwtToken, userId, validity);

        // Act & Assert
        assertTrue(session.isExpired());
    }

    @Test
    void isValid_正常系_有効なトークンの場合trueを返す() {
        // Arrange
        String jwtToken = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...";
        UUID userId = UUID.randomUUID();
        Duration validity = Duration.ofHours(24);
        AuthSession session = AuthSession.create(jwtToken, userId, validity);

        // Act & Assert
        assertTrue(session.isValid());
    }

    @Test
    void isValid_異常系_トークンがnullの場合falseを返す() {
        // Arrange
        String jwtToken = null;
        UUID userId = UUID.randomUUID();
        Instant expiresAt = Instant.now().plusSeconds(3600);
        AuthSession session = new AuthSession(jwtToken, userId, expiresAt);

        // Act & Assert
        assertFalse(session.isValid());
    }

    @Test
    void isValid_異常系_トークンが空文字の場合falseを返す() {
        // Arrange
        String jwtToken = "";
        UUID userId = UUID.randomUUID();
        Instant expiresAt = Instant.now().plusSeconds(3600);
        AuthSession session = new AuthSession(jwtToken, userId, expiresAt);

        // Act & Assert
        assertFalse(session.isValid());
    }

    @Test
    void isValid_異常系_有効期限切れの場合falseを返す() {
        // Arrange
        String jwtToken = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...";
        UUID userId = UUID.randomUUID();
        Duration validity = Duration.ofSeconds(-1); // 過去の時刻
        AuthSession session = AuthSession.create(jwtToken, userId, validity);

        // Act & Assert
        assertFalse(session.isValid());
    }
}
