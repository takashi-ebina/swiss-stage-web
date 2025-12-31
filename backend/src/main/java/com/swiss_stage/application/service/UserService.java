package com.swiss_stage.application.service;

import com.swiss_stage.domain.model.User;
import com.swiss_stage.domain.repository.UserRepository;
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

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    /**
     * ユーザーを検索または作成（findOrCreateパターン）
     * 既存ユーザーの場合はlastLoginAtを更新、新規ユーザーの場合は自動登録
     * 
     * @param googleId Google OAuth2のSub
     * @param email メールアドレス
     * @param displayName 表示名
     * @return User
     */
    public User findOrCreateUser(String googleId, String email, String displayName) {
        Optional<User> existingUser = userRepository.findByGoogleId(googleId);

        if (existingUser.isPresent()) {
            // 既存ユーザーの場合、最終ログイン日時を更新
            User user = existingUser.get();
            user.updateLastLoginAt(Instant.now());
            return userRepository.save(user);
        } else {
            // 新規ユーザーの場合、自動登録
            User newUser = User.create(UUID.randomUUID(), googleId, email, displayName);
            return userRepository.save(newUser);
        }
    }

    /**
     * ユーザーIDでユーザーを検索
     * 
     * @param userId ユーザーID
     * @return User（Optional）
     */
    public Optional<User> findById(UUID userId) {
        return userRepository.findById(userId);
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
}
