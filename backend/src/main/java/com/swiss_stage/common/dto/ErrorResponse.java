package com.swiss_stage.common.dto;

import java.time.LocalDateTime;

/**
 * エラーレスポンスDTO
 * 全APIエンドポイントで共通のエラーフォーマット
 * 憲章原則VI「コード品質とシンプリシティ」に準拠
 * 
 * Note: RecordではなくJavaBeanを使用（Jacksonのシリアライズで@JsonPropertyが必要な場合があるため）
 */
public class ErrorResponse {
    
    private String error;
    private String message;
    private String path;
    private LocalDateTime timestamp;
    
    public ErrorResponse() {
        this.timestamp = LocalDateTime.now();
    }
    
    public ErrorResponse(String error, String message, String path) {
        this.error = error;
        this.message = message;
        this.path = path;
        this.timestamp = LocalDateTime.now();
    }
    
    // Getters and Setters
    public String getError() {
        return error;
    }
    
    public void setError(String error) {
        this.error = error;
    }
    
    public String getMessage() {
        return message;
    }
    
    public void setMessage(String message) {
        this.message = message;
    }
    
    public String getPath() {
        return path;
    }
    
    public void setPath(String path) {
        this.path = path;
    }
    
    public LocalDateTime getTimestamp() {
        return timestamp;
    }
    
    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }
}
