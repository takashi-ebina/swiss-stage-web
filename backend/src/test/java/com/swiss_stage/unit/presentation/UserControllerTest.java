package com.swiss_stage.unit.presentation;

import com.swiss_stage.application.dto.DeleteAccountRequest;
import com.swiss_stage.application.dto.UserDto;
import com.swiss_stage.application.service.UserService;
import com.swiss_stage.common.exception.BusinessException;
import com.swiss_stage.common.exception.UnauthorizedException;
import com.swiss_stage.presentation.controller.UserController;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * UserControllerのユニットテスト
 * TDD: Red-Green-Refactor
 */
class UserControllerTest {

    @Mock
    private UserService userService;

    @Mock
    private SecurityContext securityContext;

    @Mock
    private Authentication authentication;

    private UserController userController;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        userController = new UserController(userService);
        
        // SecurityContextのモック設定
        SecurityContextHolder.setContext(securityContext);
        when(securityContext.getAuthentication()).thenReturn(authentication);
    }

    @Test
    void getUser_正常系_ユーザー情報を取得できる() {
        // Arrange
        UUID userId = UUID.randomUUID();
        String userIdStr = userId.toString();
        UserDto userDto = new UserDto(userId, "テストユーザー", Instant.now(), Instant.now());
        
        when(authentication.getName()).thenReturn(userIdStr);
        when(authentication.isAuthenticated()).thenReturn(true);
        when(userService.findById(userIdStr)).thenReturn(Optional.of(userDto));

        // Act
        ResponseEntity<UserDto> response = userController.getUser(userIdStr);

        // Assert
        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(userId, response.getBody().getUserId());
        verify(userService, times(1)).findById(userIdStr);
    }

    @Test
    void getUser_異常系_ユーザーが存在しない() {
        // Arrange
        UUID userId = UUID.randomUUID();
        String userIdStr = userId.toString();
        
        when(authentication.getName()).thenReturn(userIdStr);
        when(authentication.isAuthenticated()).thenReturn(true);
        when(userService.findById(userIdStr)).thenReturn(Optional.empty());

        // Act & Assert
        BusinessException exception = assertThrows(BusinessException.class, () -> {
            userController.getUser(userIdStr);
        });
        
        assertEquals("User not found", exception.getMessage());
        verify(userService, times(1)).findById(userIdStr);
    }

    @Test
    void getUser_異常系_他人のアカウントにアクセスできない() {
        // Arrange
        UUID userId = UUID.randomUUID();
        UUID otherUserId = UUID.randomUUID();
        String userIdStr = userId.toString();
        String otherUserIdStr = otherUserId.toString();
        
        when(authentication.getName()).thenReturn(userIdStr);
        when(authentication.isAuthenticated()).thenReturn(true);

        // Act & Assert
        UnauthorizedException exception = assertThrows(UnauthorizedException.class, () -> {
            userController.getUser(otherUserIdStr);
        });
        
        assertEquals("Access denied: You can only access your own account", exception.getMessage());
        verify(userService, never()).findById(anyString());
    }

    @Test
    void deleteAccount_正常系_アカウントを削除できる() {
        // Arrange
        UUID userId = UUID.randomUUID();
        String userIdStr = userId.toString();
        String email = "user@example.com";
        String confirmation = "DELETE";
        DeleteAccountRequest request = new DeleteAccountRequest(email, confirmation);
        
        when(authentication.getName()).thenReturn(userIdStr);
        when(authentication.isAuthenticated()).thenReturn(true);
        doNothing().when(userService).deleteAccount(userIdStr, email, confirmation);

        // Act
        ResponseEntity<Void> response = userController.deleteAccount(userIdStr, request);

        // Assert
        assertNotNull(response);
        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
        verify(userService, times(1)).deleteAccount(userIdStr, email, confirmation);
    }

    @Test
    void deleteAccount_異常系_メールアドレスが一致しない() {
        // Arrange
        UUID userId = UUID.randomUUID();
        String userIdStr = userId.toString();
        String wrongEmail = "wrong@example.com";
        String confirmation = "DELETE";
        DeleteAccountRequest request = new DeleteAccountRequest(wrongEmail, confirmation);
        
        when(authentication.getName()).thenReturn(userIdStr);
        when(authentication.isAuthenticated()).thenReturn(true);
        doThrow(new BusinessException("Email address does not match"))
                .when(userService).deleteAccount(userIdStr, wrongEmail, confirmation);

        // Act & Assert
        BusinessException exception = assertThrows(BusinessException.class, () -> {
            userController.deleteAccount(userIdStr, request);
        });
        
        assertEquals("Email address does not match", exception.getMessage());
        verify(userService, times(1)).deleteAccount(userIdStr, wrongEmail, confirmation);
    }

    @Test
    void deleteAccount_異常系_確認文字列が一致しない() {
        // Arrange
        UUID userId = UUID.randomUUID();
        String userIdStr = userId.toString();
        String email = "user@example.com";
        String wrongConfirmation = "delete";
        DeleteAccountRequest request = new DeleteAccountRequest(email, wrongConfirmation);
        
        when(authentication.getName()).thenReturn(userIdStr);
        when(authentication.isAuthenticated()).thenReturn(true);
        doThrow(new BusinessException("Invalid confirmation string"))
                .when(userService).deleteAccount(userIdStr, email, wrongConfirmation);

        // Act & Assert
        BusinessException exception = assertThrows(BusinessException.class, () -> {
            userController.deleteAccount(userIdStr, request);
        });
        
        assertEquals("Invalid confirmation string", exception.getMessage());
        verify(userService, times(1)).deleteAccount(userIdStr, email, wrongConfirmation);
    }

    @Test
    void deleteAccount_異常系_他人のアカウントは削除できない() {
        // Arrange
        UUID userId = UUID.randomUUID();
        UUID otherUserId = UUID.randomUUID();
        String userIdStr = userId.toString();
        String otherUserIdStr = otherUserId.toString();
        String email = "user@example.com";
        String confirmation = "DELETE";
        DeleteAccountRequest request = new DeleteAccountRequest(email, confirmation);
        
        when(authentication.getName()).thenReturn(userIdStr);
        when(authentication.isAuthenticated()).thenReturn(true);

        // Act & Assert
        UnauthorizedException exception = assertThrows(UnauthorizedException.class, () -> {
            userController.deleteAccount(otherUserIdStr, request);
        });
        
        assertEquals("Access denied: You can only access your own account", exception.getMessage());
        verify(userService, never()).deleteAccount(anyString(), anyString(), anyString());
    }
}
