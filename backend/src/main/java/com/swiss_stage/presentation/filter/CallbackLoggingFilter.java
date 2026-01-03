package com.swiss_stage.presentation.filter;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Collections;

/**
 * すべてのリクエストをログに記録するフィルタ（最優先で実行）
 * OAuth2コールバックを含むすべてのリクエストパスを可視化
 */
@Component
@Order(0)
public class CallbackLoggingFilter implements Filter {

    private static final Logger logger = LoggerFactory.getLogger(CallbackLoggingFilter.class);

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        if (!(request instanceof HttpServletRequest)) {
            chain.doFilter(request, response);
            return;
        }

        HttpServletRequest req = (HttpServletRequest) request;
        String uri = req.getRequestURI();
        String queryString = req.getQueryString();
        
        // すべてのリクエストをログ出力（OAuth関連とAPIは特に詳細ログ）
        if (uri != null && (uri.startsWith("/login") || uri.startsWith("/oauth2") || uri.startsWith("/api"))) {
            logger.info("REQUEST: {} {}?{}", req.getMethod(), uri, queryString);
            if (uri.startsWith("/login/oauth2/code/")) {
                logger.info("==> OAuth2 callback detected: {}?{}", uri, queryString);
                Collections.list(req.getHeaderNames()).forEach(name -> 
                    logger.debug("  Header {}: {}", name, req.getHeader(name)));
            }
        }

        chain.doFilter(request, response);
    }
}
