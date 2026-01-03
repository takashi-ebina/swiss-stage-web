package com.swiss_stage.presentation.filter;

import com.swiss_stage.application.service.JwtService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;
import java.util.UUID;

/**
 * JWT認証フィルター
 * 
 * 処理フロー:
 * 1. CookieからJWTトークンを取得
 * 2. トークンを検証してuserIdを取得
 * 3. SecurityContextにAuthentication設定
 * 4. 次のフィルターに処理を渡す
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final Logger logger = LoggerFactory.getLogger(JwtAuthenticationFilter.class);

    private final JwtService jwtService;

    public JwtAuthenticationFilter(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        try {
            String jwt = extractJwtFromCookie(request);

            if (jwt != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                UUID userId = jwtService.validateTokenAndGetUserId(jwt);

                if (userId != null) {
                    UsernamePasswordAuthenticationToken authentication =
                            new UsernamePasswordAuthenticationToken(userId.toString(), null, Collections.emptyList());
                    authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                    SecurityContextHolder.getContext().setAuthentication(authentication);

                    logger.debug("JWT authentication successful for userId={}", userId);
                }
            }
        } catch (Exception e) {
            logger.warn("JWT authentication failed: {}", e.getMessage());
        }

        filterChain.doFilter(request, response);
    }

    /**
     * CookieからJWTトークンを抽出
     * 
     * @param request HTTPリクエスト
     * @return JWTトークン（存在しない場合はnull）
     */
    private String extractJwtFromCookie(HttpServletRequest request) {
        if (request.getCookies() == null) {
            return null;
        }

        for (Cookie cookie : request.getCookies()) {
            if ("jwt_token".equals(cookie.getName())) {
                return cookie.getValue();
            }
        }

        return null;
    }
}
