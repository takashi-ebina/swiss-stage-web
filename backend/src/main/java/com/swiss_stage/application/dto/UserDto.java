package com.swiss_stage.application.dto;

import com.swiss_stage.domain.model.User;

import java.time.Instant;
import java.util.UUID;

/**
 * ユーザー情報DTO
 * 憲章原則VI「個人情報保護とプライバシー」に準拠
 * email/googleIdは返さない（個人情報保護）
 * 
 * Note: RecordではなくJavaBeanを使用（Jacksonのデフォルトコンストラクタとセッターが必要）
 */
public class UserDto {
    
    private UUID userId;
    private String displayName;
    private Instant createdAt;
    private Instant lastLoginAt;
    
    public UserDto() {
    }
    
    public UserDto(UUID userId, String displayName, Instant createdAt, Instant lastLoginAt) {
        this.userId = userId;
        this.displayName = displayName;
        this.createdAt = createdAt;
        this.lastLoginAt = lastLoginAt;
    }
    
    /**
     * UserエンティティからDTOを作成
     */
    public static UserDto fromUser(User user) {
        return new UserDto(
            user.getUserId(),
            user.getDisplayName(),
            user.getCreatedAt(),
            user.getLastLoginAt()
        );
    }
    
    // Getters and Setters
    public UUID getUserId() {
        return userId;
    }
    
    public void setUserId(UUID userId) {
        this.userId = userId;
    }
    
    public String getDisplayName() {
        return displayName;
    }
    
    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }
    
    public Instant getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
    
    public Instant getLastLoginAt() {
        return lastLoginAt;
    }
    
    public void setLastLoginAt(Instant lastLoginAt) {
        this.lastLoginAt = lastLoginAt;
    }
}
