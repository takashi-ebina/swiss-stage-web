package com.swiss_stage.common.exception;

/**
 * 認証・認可エラーを表す例外クラス
 * HTTP 401 Unauthorized を返す
 */
public class UnauthorizedException extends RuntimeException {
    
    public UnauthorizedException(String message) {
        super(message);
    }
    
    public UnauthorizedException(String message, Throwable cause) {
        super(message, cause);
    }
}
