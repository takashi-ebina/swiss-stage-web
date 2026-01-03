package com.swiss_stage.common.util;

/**
 * 個人情報マスキングユーティリティ
 * 憲章原則VI「個人情報保護とプライバシー」に準拠
 * ログ出力時にemail/displayNameをマスキングする
 */
public class LoggingUtil {

    private LoggingUtil() {
        // Utility class - private constructor
    }

    /**
     * メールアドレスをマスキングする
     * 例: user@example.com → [MASKED_EMAIL]
     *
     * @param email メールアドレス
     * @return マスキングされた文字列
     */
    public static String maskEmail(String email) {
        if (email == null || email.isBlank()) {
            return "[MASKED_EMAIL]";
        }
        return "[MASKED_EMAIL]";
    }

    /**
     * 名前をマスキングする
     * 例: John Doe → [MASKED_NAME]
     *
     * @param name 名前
     * @return マスキングされた文字列
     */
    public static String maskName(String name) {
        if (name == null || name.isBlank()) {
            return "[MASKED_NAME]";
        }
        return "[MASKED_NAME]";
    }

    /**
     * ユーザーIDをログ出力用にフォーマットする
     * userIdは個人識別情報ではないため、マスキング不要
     *
     * @param userId ユーザーID
     * @return フォーマットされたユーザーID
     */
    public static String formatUserId(String userId) {
        if (userId == null || userId.isBlank()) {
            return "[UNKNOWN_USER]";
        }
        return userId;
    }
}
