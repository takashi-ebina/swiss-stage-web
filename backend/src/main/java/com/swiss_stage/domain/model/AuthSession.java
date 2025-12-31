package com.swiss_stage.domain.model;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

/**
 * AuthSession値オブジェクト（ドメインモデル）
 * 認証セッション情報を表す値オブジェクト
 * JWTトークンとしてHTTP-only Cookieに保存される（DynamoDBには保存しない）
 * 憲章原則I「ドメイン駆動設計」、原則VI「コード品質とシンプリシティ」に準拠
 * Java標準Recordを使用（不変性を言語レベルで保証）
 */
public record AuthSession(
        String jwtToken,
        UUID userId,
        Instant expiresAt
) {
    /**
     * ファクトリメソッド（JWT生成時に作成）
     * @param jwtToken JWTトークン文字列
     * @param userId ユーザーID
     * @param validity 有効期限（Duration）
     * @return 新規AuthSessionオブジェクト
     */
    public static AuthSession create(String jwtToken, UUID userId, Duration validity) {
        Instant expiresAt = Instant.now().plus(validity);
        return new AuthSession(jwtToken, userId, expiresAt);
    }

    /**
     * 有効期限チェック
     * @return 有効期限切れの場合true
     */
    public boolean isExpired() {
        return Instant.now().isAfter(expiresAt);
    }

    /**
     * トークン検証（署名検証はJwtServiceで実施）
     * @return 有効なトークンの場合true
     */
    public boolean isValid() {
        return jwtToken != null && !jwtToken.isEmpty() && !isExpired();
    }
}
