package com.swiss_stage.unit.domain;

import com.swiss_stage.domain.model.User;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Userエンティティのドメインロジックをテスト
 * TDD: Red-Green-Refactor
 */
class UserTest {

    @Test
    void create_正常系_新規ユーザーを作成できる() {
        // Arrange
        UUID userId = UUID.randomUUID();
        String googleId = "102345678901234567890";
        String email = "user@example.com";
        String displayName = "山田太郎";

        // Act
        User user = User.create(userId, googleId, email, displayName);

        // Assert
        assertNotNull(user);
        assertEquals(userId, user.getUserId());
        assertEquals(googleId, user.getGoogleId());
        assertEquals(email, user.getEmail());
        assertEquals(displayName, user.getDisplayName());
        assertNotNull(user.getCreatedAt());
        assertNotNull(user.getLastLoginAt());
        assertEquals(user.getCreatedAt(), user.getLastLoginAt());
    }

    @Test
    void create_異常系_GoogleIDがnullの場合例外をスローする() {
        // Arrange
        UUID userId = UUID.randomUUID();
        String googleId = null;
        String email = "user@example.com";
        String displayName = "山田太郎";

        // Act & Assert
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> User.create(userId, googleId, email, displayName)
        );
        assertEquals("Google ID must not be empty", exception.getMessage());
    }

    @Test
    void create_異常系_GoogleIDが空文字の場合例外をスローする() {
        // Arrange
        UUID userId = UUID.randomUUID();
        String googleId = "";
        String email = "user@example.com";
        String displayName = "山田太郎";

        // Act & Assert
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> User.create(userId, googleId, email, displayName)
        );
        assertEquals("Google ID must not be empty", exception.getMessage());
    }

    @Test
    void create_異常系_メールアドレスが不正な形式の場合例外をスローする() {
        // Arrange
        UUID userId = UUID.randomUUID();
        String googleId = "102345678901234567890";
        String email = "invalid-email";
        String displayName = "山田太郎";

        // Act & Assert
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> User.create(userId, googleId, email, displayName)
        );
        assertEquals("Invalid email format", exception.getMessage());
    }

    @Test
    void create_異常系_displayNameがnullの場合例外をスローする() {
        // Arrange
        UUID userId = UUID.randomUUID();
        String googleId = "102345678901234567890";
        String email = "user@example.com";
        String displayName = null;

        // Act & Assert
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> User.create(userId, googleId, email, displayName)
        );
        assertEquals("Display name must be 1-100 characters", exception.getMessage());
    }

    @Test
    void create_異常系_displayNameが101文字以上の場合例外をスローする() {
        // Arrange
        UUID userId = UUID.randomUUID();
        String googleId = "102345678901234567890";
        String email = "user@example.com";
        String displayName = "a".repeat(101);

        // Act & Assert
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> User.create(userId, googleId, email, displayName)
        );
        assertEquals("Display name must be 1-100 characters", exception.getMessage());
    }

    @Test
    void updateLastLoginAt_正常系_最終ログイン日時を更新できる() {
        // Arrange
        UUID userId = UUID.randomUUID();
        String googleId = "102345678901234567890";
        String email = "user@example.com";
        String displayName = "山田太郎";
        User user = User.create(userId, googleId, email, displayName);
        
        Instant originalLastLoginAt = user.getLastLoginAt();
        
        // Act
        Instant newLastLoginAt = Instant.now().plusSeconds(60);
        user.updateLastLoginAt(newLastLoginAt);

        // Assert
        assertNotEquals(originalLastLoginAt, user.getLastLoginAt());
        assertEquals(newLastLoginAt, user.getLastLoginAt());
    }
}
