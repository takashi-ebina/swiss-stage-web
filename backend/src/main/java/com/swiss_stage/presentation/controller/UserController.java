package com.swiss_stage.presentation.controller;

import com.swiss_stage.application.dto.DeleteAccountRequest;
import com.swiss_stage.application.dto.UserDto;
import com.swiss_stage.application.service.UserService;
import com.swiss_stage.common.exception.BusinessException;
import com.swiss_stage.common.exception.UnauthorizedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

/**
 * ユーザー関連のAPIエンドポイント
 * 
 * エンドポイント:
 * - GET /api/users/{userId}: ユーザー情報を取得
 * - DELETE /api/users/{userId}: アカウントを削除
 */
@RestController
@RequestMapping("/api/users")
public class UserController {

    private static final Logger logger = LoggerFactory.getLogger(UserController.class);

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    /**
     * ユーザー情報を取得
     * 
     * @param userId ユーザーID
     * @return ユーザー情報
     */
    @GetMapping("/{userId}")
    public ResponseEntity<UserDto> getUser(@PathVariable String userId) {
        // 自分のuserIdのみアクセス可能
        validateUserAccess(userId);

        UserDto user = userService.findById(userId)
                .orElseThrow(() -> new BusinessException("User not found"));

        return ResponseEntity.ok(user);
    }

    /**
     * アカウントを削除
     * 
     * @param userId ユーザーID
     * @param request 削除リクエスト
     * @return 削除成功（204 No Content）
     */
    @DeleteMapping("/{userId}")
    public ResponseEntity<Void> deleteAccount(
            @PathVariable String userId,
            @RequestBody DeleteAccountRequest request) {
        // 自分のuserIdのみ削除可能
        validateUserAccess(userId);

        userService.deleteAccount(userId, request.email(), request.confirmation());

        logger.info("Account deletion successful. userId={}", userId);

        return ResponseEntity.noContent().build();
    }

    /**
     * ユーザーアクセス検証
     * 現在認証されているユーザーが指定されたuserIdにアクセス可能かを確認
     * 
     * @param userId 対象ユーザーID
     */
    private void validateUserAccess(String userId) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new UnauthorizedException("User is not authenticated");
        }

        String currentUserId = authentication.getName();
        
        if (!currentUserId.equals(userId)) {
            throw new UnauthorizedException("Access denied: You can only access your own account");
        }
    }
}
