package com.swiss_stage.common.exception;

/**
 * ビジネスロジックエラーを表す例外クラス
 * HTTP 400 Bad Request または 409 Conflict を返す
 */
public class BusinessException extends RuntimeException {
    
    private final String errorCode;
    
    public BusinessException(String message) {
        super(message);
        this.errorCode = "BUSINESS_ERROR";
    }
    
    public BusinessException(String message, String errorCode) {
        super(message);
        this.errorCode = errorCode;
    }
    
    public BusinessException(String message, Throwable cause) {
        super(message, cause);
        this.errorCode = "BUSINESS_ERROR";
    }
    
    public String getErrorCode() {
        return errorCode;
    }
}
