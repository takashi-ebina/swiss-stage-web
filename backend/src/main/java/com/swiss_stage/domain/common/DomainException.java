package com.swiss_stage.domain.common;

/**
 * ドメイン層で発生するビジネスルール違反を表す例外
 * 
 * この例外は、ドメインエンティティや値オブジェクトの不変条件が
 * 違反された場合や、ビジネスルールに反する操作が行われた場合にスローされる。
 * 
 * 例:
 * - 参加者数が上限（32名）を超える場合
 * - 必須項目が欠落している場合
 * - 不正な値が設定された場合
 */
public class DomainException extends RuntimeException {

    /**
     * メッセージ付きコンストラクタ
     * 
     * @param message エラーメッセージ
     */
    public DomainException(String message) {
        super(message);
    }

    /**
     * メッセージと原因付きコンストラクタ
     * 
     * @param message エラーメッセージ
     * @param cause 原因となる例外
     */
    public DomainException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * 原因付きコンストラクタ
     * 
     * @param cause 原因となる例外
     */
    public DomainException(Throwable cause) {
        super(cause);
    }
}
