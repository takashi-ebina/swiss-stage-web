package com.swiss_stage.application.service;

import com.swiss_stage.application.dto.UserDto;
import com.swiss_stage.common.exception.BusinessException;
import com.swiss_stage.domain.model.User;
import com.swiss_stage.domain.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

/**
 * ユーザーサービス（Application層）
 * 憲章原則I「ドメイン駆動設計」に準拠
 */
@Service
public class UserService {

    private static final Logger logger = LoggerFactory.getLogger(UserService.class);

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    /**
     * ユーザーを検索または作成（findOrCreateパターン）- DTOバージョン
     * 既存ユーザーの場合はlastLoginAtを更新、新規ユーザーの場合は自動登録
     * 
     * @param googleId Google OAuth2のSub
     * @param email メールアドレス
     * @param displayName 表示名
     * @return UserDto
     */
    public UserDto findOrCreateUser(String googleId, String email, String displayName) {
        Optional<User> existingUser = userRepository.findByGoogleId(googleId);

        User user;
        if (existingUser.isPresent()) {
            // 既存ユーザーの場合、最終ログイン日時を更新
            user = existingUser.get();
            user.updateLastLoginAt(Instant.now());
            user = userRepository.save(user);
        } else {
            // 新規ユーザーの場合、自動登録
            user = User.create(UUID.randomUUID(), googleId, email, displayName);
            user = userRepository.save(user);
        }

        return convertToDto(user);
    }

    /**
     * ユーザーIDでユーザーを検索 - DTOバージョン
     * 
     * @param userId ユーザーID（文字列形式）
     * @return UserDto（Optional）
     */
    public Optional<UserDto> findById(String userId) {
        try {
            UUID uuid = UUID.fromString(userId);
            return userRepository.findById(uuid)
                    .map(this::convertToDto);
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
    }

    /**
     * ユーザーを保存
     * 
     * @param user User
     * @return 保存されたUser
     */
    public User save(User user) {
        return userRepository.save(user);
    }

    /**
     * ユーザーを削除
     * 
     * @param userId ユーザーID
     */
    public void deleteById(UUID userId) {
        userRepository.deleteById(userId);
    }

    /**
     * アカウント削除
     * 進行中のトーナメントが存在する場合はエラー（今後の実装）
     * メールアドレス確認と削除を実行
     * 
     * @param userId ユーザーID（文字列形式）
     * @param email 確認用メールアドレス
     * @param confirmation 削除確認文字列
     */
    public void deleteAccount(String userId, String email, String confirmation) {
        UUID uuid;
        try {
            uuid = UUID.fromString(userId);
        } catch (IllegalArgumentException e) {
            throw new BusinessException("Invalid user ID format");
        }

        // ユーザー存在確認
        User user = userRepository.findById(uuid)
                .orElseThrow(() -> new BusinessException("User not found"));

        // メールアドレス確認
        if (!user.getEmail().equals(email)) {
            throw new BusinessException("Email address does not match");
        }

        // 削除確認文字列チェック
        if (!"DELETE".equals(confirmation)) {
            throw new BusinessException("Invalid confirmation string");
        }

        // TODO: 進行中のトーナメント存在チェック（今後の実装）
        // if (hasPendingTournaments(uuid)) {
        //     throw new BusinessException("Cannot delete account with pending tournaments");
        // }

        // ユーザー削除
        userRepository.deleteById(uuid);

        logger.info("User account deleted. userId={}", userId);
    }

    /**
     * UserエンティティをUserDtoに変換
     * 
     * @param user User
     * @return UserDto
     */
    private UserDto convertToDto(User user) {
        return new UserDto(
                user.getUserId(),
                user.getDisplayName(),
                user.getCreatedAt(),
                user.getLastLoginAt()
        );
    }
}
