package com.swiss_stage.presentation.handler;

import com.swiss_stage.application.dto.UserDto;
import com.swiss_stage.application.service.JwtService;
import com.swiss_stage.application.service.UserService;
import com.swiss_stage.common.util.LoggingUtil;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * OAuth2認証成功時のハンドラー
 * 
 * 処理フロー:
 * 1. OAuth2UserからGoogle IDとユーザー情報を取得
 * 2. UserServiceでユーザーを検索または作成
 * 3. JWTトークンを生成
 * 4. HTTP-only Cookieにトークンを設定
 * 5. ダッシュボードにリダイレクト
 */
@Component
public class OAuth2AuthenticationSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private static final Logger logger = LoggerFactory.getLogger(OAuth2AuthenticationSuccessHandler.class);

    private final UserService userService;
    private final JwtService jwtService;
    private final String frontendUrl;

    public OAuth2AuthenticationSuccessHandler(
            UserService userService,
            JwtService jwtService,
            @Value("${app.frontend.url:http://localhost:3000}") String frontendUrl) {
        this.userService = userService;
        this.jwtService = jwtService;
        this.frontendUrl = frontendUrl;
        logger.info("OAuth2AuthenticationSuccessHandler created: instance={}", this);
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException, ServletException {
        logger.info(">>> OAuth2AuthenticationSuccessHandler.onAuthenticationSuccess CALLED <<<");
        
        OAuth2User oauth2User = (OAuth2User) authentication.getPrincipal();
        
        // Google IDとユーザー情報を取得
        String googleId = oauth2User.getAttribute("sub");
        String email = oauth2User.getAttribute("email");
        String name = oauth2User.getAttribute("name");

        logger.debug("OAuth2 authentication successful for Google ID: {}", LoggingUtil.maskEmail(email));

        // ユーザーを検索または作成
        UserDto user = userService.findOrCreateUser(googleId, email, name);

        // JWTトークンを生成
        String token = jwtService.generateToken(user.getUserId());

        // Set-Cookieを手動で設定してSameSite/Secureを環境に応じて制御
        int maxAge = 24 * 60 * 60; // 24時間
        StringBuilder cookieBuilder = new StringBuilder();
        cookieBuilder.append("jwt_token=").append(token)
                .append("; Path=/")
                .append("; Max-Age=").append(maxAge)
                .append("; HttpOnly");

        boolean secure = frontendUrl != null && frontendUrl.startsWith("https");
        if (secure) {
            cookieBuilder.append("; Secure; SameSite=None");
        } else {
            // ローカルHTTP環境向け（WSL/localhostなど）では Secure を無効化し SameSite=Lax を使用
            cookieBuilder.append("; SameSite=Lax");
        }

        response.setHeader("Set-Cookie", cookieBuilder.toString());

        logger.info("User authenticated successfully. userId={}", user.getUserId());

        // ダッシュボードにリダイレクト
        getRedirectStrategy().sendRedirect(request, response, frontendUrl + "/dashboard");
    }
}
