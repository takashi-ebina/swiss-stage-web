package com.swiss_stage.presentation.handler;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;

/**
 * OAuth2認証失敗時のハンドラー
 * 
 * 処理フロー:
 * 1. エラーコードを判定
 * 2. ユーザーフレンドリーなエラーメッセージを生成
 * 3. ログイン画面にリダイレクト（エラーメッセージをクエリパラメータで渡す）
 */
@Component
public class OAuth2AuthenticationFailureHandler extends SimpleUrlAuthenticationFailureHandler {

    private static final Logger logger = LoggerFactory.getLogger(OAuth2AuthenticationFailureHandler.class);

    private final String frontendUrl;

    public OAuth2AuthenticationFailureHandler(
            @Value("${app.frontend.url:http://localhost:3000}") String frontendUrl) {
        this.frontendUrl = frontendUrl;
    }

    @Override
    public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response,
                                        AuthenticationException exception) throws IOException, ServletException {
        String errorCode = determineErrorCode(exception);
        
        logger.warn("OAuth2 authentication failed. errorCode={}, message={}", 
                errorCode, exception.getMessage());

        // ログイン画面にリダイレクト（エラーコードをクエリパラメータで渡す）
        String redirectUrl = UriComponentsBuilder.fromUriString(frontendUrl + "/login")
                .queryParam("error", errorCode)
                .build()
                .toUriString();

        getRedirectStrategy().sendRedirect(request, response, redirectUrl);
    }

    /**
     * 例外からエラーコードを判定
     * 
     * @param exception 認証例外
     * @return エラーコード
     */
    private String determineErrorCode(AuthenticationException exception) {
        if (exception instanceof OAuth2AuthenticationException) {
            OAuth2AuthenticationException oauth2Exception = (OAuth2AuthenticationException) exception;
            String errorCode = oauth2Exception.getError().getErrorCode();

            // OAuth2エラーコードをマッピング
            switch (errorCode) {
                case "access_denied":
                    return "access_denied";
                case "invalid_client":
                    return "invalid_client";
                case "unauthorized_client":
                    return "unauthorized_client";
                case "invalid_grant":
                    return "invalid_grant";
                default:
                    return "unknown_error";
            }
        }

        // ネットワークエラーやタイムアウト
        if (exception.getCause() != null) {
            String causeMessage = exception.getCause().getMessage();
            if (causeMessage != null && causeMessage.contains("timeout")) {
                return "network_error";
            }
        }

        return "unknown_error";
    }
}
