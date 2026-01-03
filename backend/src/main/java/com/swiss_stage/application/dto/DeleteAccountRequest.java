package com.swiss_stage.application.dto;

/**
 * アカウント削除リクエストDTO
 * 
 * フィールド:
 * - email: 確認用メールアドレス
 * - confirmation: 削除確認文字列（"DELETE"）
 */
public record DeleteAccountRequest(
        String email,
        String confirmation
) {
}
