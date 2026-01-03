package com.swiss_stage.presentation.controller;

import com.swiss_stage.application.dto.UserDto;
import com.swiss_stage.application.service.UserService;
import com.swiss_stage.common.exception.UnauthorizedException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

/**
 * 認証関連のAPIエンドポイント
 * 
 * エンドポイント:
 * - GET /api/auth/me: 現在のユーザー情報を取得
 * - POST /api/auth/logout: ログアウト処理
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private static final Logger logger = LoggerFactory.getLogger(AuthController.class);

    private final UserService userService;

    public AuthController(UserService userService) {
        this.userService = userService;
    }

    /**
     * 現在認証されているユーザーの情報を取得
     * 
     * @return ユーザー情報
     */
    @GetMapping("/me")
    public ResponseEntity<UserDto> getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new UnauthorizedException("User is not authenticated");
        }

        String userId = authentication.getName();
        UserDto user = userService.findById(userId)
                .orElseThrow(() -> new UnauthorizedException("User not found"));

        logger.debug("Current user retrieved. userId={}", userId);
        
        return ResponseEntity.ok(user);
    }

    /**
     * ログアウト処理
     * JWTトークンのCookieを削除し、セッションをクリア
     * 
     * @param response HTTPレスポンス
     * @return 成功メッセージ
     */
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpServletResponse response) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        
        if (authentication != null) {
            String userId = authentication.getName();
            logger.info("User logged out. userId={}", userId);
        }

        // JWTトークンのCookieを削除
        Cookie cookie = new Cookie("jwt_token", null);
        cookie.setHttpOnly(true);
        cookie.setSecure(true);
        cookie.setPath("/");
        cookie.setMaxAge(0); // Cookie削除

        response.addCookie(cookie);

        // セキュリティコンテキストをクリア
        SecurityContextHolder.clearContext();

        return ResponseEntity.noContent().build();
    }
}
