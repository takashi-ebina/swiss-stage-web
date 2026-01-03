package com.swiss_stage.domain.repository;

import com.swiss_stage.domain.model.User;

import java.util.Optional;
import java.util.UUID;

/**
 * UserRepositoryインターフェース（ドメイン層）
 * 憲章原則I「ドメイン駆動設計」に準拠
 * 実装はinfrastructure層に配置
 */
public interface UserRepository {

    /**
     * ユーザーIDでユーザーを検索
     * @param userId ユーザーID
     * @return User（存在しない場合はOptional.empty()）
     */
    Optional<User> findById(UUID userId);

    /**
     * Google IDでユーザーを検索
     * @param googleId Google OAuth2のSub
     * @return User（存在しない場合はOptional.empty()）
     */
    Optional<User> findByGoogleId(String googleId);

    /**
     * ユーザーを保存（新規作成または更新）
     * @param user Userエンティティ
     * @return 保存されたUser
     */
    User save(User user);

    /**
     * ユーザーIDでユーザーを削除
     * @param userId ユーザーID
     */
    void deleteById(UUID userId);
}
