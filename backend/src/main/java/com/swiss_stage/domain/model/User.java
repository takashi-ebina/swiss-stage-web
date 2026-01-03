package com.swiss_stage.domain.model;

import java.time.Instant;
import java.util.UUID;

/**
 * Userエンティティ（ドメインモデル）
 * 大会運営者を表すドメインエンティティ
 * 憲章原則I「ドメイン駆動設計」、原則VI「コード品質とシンプリシティ」に準拠
 * ミュータブルな状態（lastLoginAt更新）が必要なため標準Javaクラスで実装
 */
public class User {
    private final UUID userId;
    private final String googleId;
    private final String email;
    private final String displayName;
    private final Instant createdAt;
    private Instant lastLoginAt;

    private User(UUID userId, String googleId, String email, String displayName, Instant createdAt, Instant lastLoginAt) {
        this.userId = userId;
        this.googleId = googleId;
        this.email = email;
        this.displayName = displayName;
        this.createdAt = createdAt;
        this.lastLoginAt = lastLoginAt;
    }

    /**
     * ファクトリメソッド（新規ユーザー作成）
     * @param userId ユーザーID（UUID）
     * @param googleId Google OAuth2のSub
     * @param email メールアドレス
     * @param displayName 表示名
     * @return 新規Userエンティティ
     */
    public static User create(UUID userId, String googleId, String email, String displayName) {
        validateGoogleId(googleId);
        validateEmail(email);
        validateDisplayName(displayName);

        Instant now = Instant.now();
        return new User(userId, googleId, email, displayName, now, now);
    }

    /**
     * ファクトリメソッド（既存データからの復元）
     * @param userId ユーザーID（UUID）
     * @param googleId Google OAuth2のSub
     * @param email メールアドレス
     * @param displayName 表示名
     * @param createdAt 作成日時
     * @param lastLoginAt 最終ログイン日時
     * @return Userエンティティ
     */
    public static User restore(UUID userId, String googleId, String email, String displayName, 
                               Instant createdAt, Instant lastLoginAt) {
        return new User(userId, googleId, email, displayName, createdAt, lastLoginAt);
    }

    /**
     * 最終ログイン日時更新
     * @param lastLoginAt 最終ログイン日時
     */
    public void updateLastLoginAt(Instant lastLoginAt) {
        this.lastLoginAt = lastLoginAt;
    }

    /**
     * Google ID検証
     * @param googleId Google OAuth2のSub
     */
    private static void validateGoogleId(String googleId) {
        if (googleId == null || googleId.isEmpty()) {
            throw new IllegalArgumentException("Google ID must not be empty");
        }
    }

    /**
     * メールアドレス検証
     * @param email メールアドレス
     */
    private static void validateEmail(String email) {
        if (email == null || !email.matches("^[A-Za-z0-9+_.-]+@(.+)$")) {
            throw new IllegalArgumentException("Invalid email format");
        }
    }

    /**
     * 表示名検証
     * @param displayName 表示名（1-100文字）
     */
    private static void validateDisplayName(String displayName) {
        if (displayName == null || displayName.isEmpty() || displayName.length() > 100) {
            throw new IllegalArgumentException("Display name must be 1-100 characters");
        }
    }

    // Getters
    public UUID getUserId() {
        return userId;
    }

    public String getGoogleId() {
        return googleId;
    }

    public String getEmail() {
        return email;
    }

    public String getDisplayName() {
        return displayName;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getLastLoginAt() {
        return lastLoginAt;
    }
}
