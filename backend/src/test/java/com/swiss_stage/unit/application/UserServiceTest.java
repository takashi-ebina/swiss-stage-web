package com.swiss_stage.unit.application;

import com.swiss_stage.application.dto.UserDto;
import com.swiss_stage.application.service.UserService;
import com.swiss_stage.common.exception.BusinessException;
import com.swiss_stage.domain.model.User;
import com.swiss_stage.domain.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * UserServiceのユニットテスト
 * TDD: Red-Green-Refactor
 */
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    private UserService userService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        userService = new UserService(userRepository);
    }

    @Test
    void findOrCreateUser_正常系_既存ユーザーを返す() {
        // Arrange
        String googleId = "102345678901234567890";
        String email = "existing@example.com";
        String displayName = "既存ユーザー";
        
        User existingUser = User.create(UUID.randomUUID(), googleId, email, displayName);
        when(userRepository.findByGoogleId(googleId)).thenReturn(Optional.of(existingUser));
        when(userRepository.save(any(User.class))).thenReturn(existingUser);

        // Act
        UserDto result = userService.findOrCreateUser(googleId, email, displayName);

        // Assert
        assertNotNull(result);
        assertEquals(existingUser.getUserId(), result.getUserId());
        assertEquals(displayName, result.getDisplayName());
        verify(userRepository, times(1)).findByGoogleId(googleId);
        verify(userRepository, times(1)).save(any(User.class)); // lastLoginAt更新のため保存
    }

    @Test
    void findOrCreateUser_正常系_新規ユーザーを作成する() {
        // Arrange
        String googleId = "102345678901234567890";
        String email = "newuser@example.com";
        String displayName = "新規ユーザー";
        
        when(userRepository.findByGoogleId(googleId)).thenReturn(Optional.empty());
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        UserDto result = userService.findOrCreateUser(googleId, email, displayName);

        // Assert
        assertNotNull(result);
        assertNotNull(result.getUserId());
        assertEquals(displayName, result.getDisplayName());
        verify(userRepository, times(1)).findByGoogleId(googleId);
        verify(userRepository, times(1)).save(any(User.class));
    }

    @Test
    void findById_正常系_ユーザーを取得できる() {
        // Arrange
        UUID userId = UUID.randomUUID();
        String googleId = "102345678901234567890";
        String email = "user@example.com";
        String displayName = "テストユーザー";
        
        User user = User.create(userId, googleId, email, displayName);
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        // Act
        Optional<UserDto> result = userService.findById(userId.toString());

        // Assert
        assertTrue(result.isPresent());
        assertEquals(userId, result.get().getUserId());
        verify(userRepository, times(1)).findById(userId);
    }

    @Test
    void findById_異常系_ユーザーが存在しない場合空のOptionalを返す() {
        // Arrange
        UUID userId = UUID.randomUUID();
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        // Act
        Optional<UserDto> result = userService.findById(userId.toString());

        // Assert
        assertFalse(result.isPresent());
        verify(userRepository, times(1)).findById(userId);
    }

    @Test
    void deleteAccount_正常系_アカウントを削除できる() {
        // Arrange
        UUID userId = UUID.randomUUID();
        String googleId = "102345678901234567890";
        String email = "user@example.com";
        String displayName = "テストユーザー";
        String confirmation = "DELETE";
        
        User user = User.create(userId, googleId, email, displayName);
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        // Act
        userService.deleteAccount(userId.toString(), email, confirmation);

        // Assert
        verify(userRepository, times(1)).findById(userId);
        verify(userRepository, times(1)).deleteById(userId);
    }

    @Test
    void deleteAccount_異常系_ユーザーIDが不正な形式() {
        // Arrange
        String invalidUserId = "invalid-uuid";
        String email = "user@example.com";
        String confirmation = "DELETE";

        // Act & Assert
        BusinessException exception = assertThrows(BusinessException.class, () -> {
            userService.deleteAccount(invalidUserId, email, confirmation);
        });
        
        assertEquals("Invalid user ID format", exception.getMessage());
        verify(userRepository, never()).findById(any());
        verify(userRepository, never()).deleteById(any());
    }

    @Test
    void deleteAccount_異常系_ユーザーが存在しない() {
        // Arrange
        UUID userId = UUID.randomUUID();
        String email = "user@example.com";
        String confirmation = "DELETE";
        
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        // Act & Assert
        BusinessException exception = assertThrows(BusinessException.class, () -> {
            userService.deleteAccount(userId.toString(), email, confirmation);
        });
        
        assertEquals("User not found", exception.getMessage());
        verify(userRepository, times(1)).findById(userId);
        verify(userRepository, never()).deleteById(any());
    }

    @Test
    void deleteAccount_異常系_メールアドレスが一致しない() {
        // Arrange
        UUID userId = UUID.randomUUID();
        String googleId = "102345678901234567890";
        String actualEmail = "actual@example.com";
        String wrongEmail = "wrong@example.com";
        String displayName = "テストユーザー";
        String confirmation = "DELETE";
        
        User user = User.create(userId, googleId, actualEmail, displayName);
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        // Act & Assert
        BusinessException exception = assertThrows(BusinessException.class, () -> {
            userService.deleteAccount(userId.toString(), wrongEmail, confirmation);
        });
        
        assertEquals("Email address does not match", exception.getMessage());
        verify(userRepository, times(1)).findById(userId);
        verify(userRepository, never()).deleteById(any());
    }

    @Test
    void deleteAccount_異常系_確認文字列が一致しない() {
        // Arrange
        UUID userId = UUID.randomUUID();
        String googleId = "102345678901234567890";
        String email = "user@example.com";
        String displayName = "テストユーザー";
        String wrongConfirmation = "delete"; // 小文字
        
        User user = User.create(userId, googleId, email, displayName);
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        // Act & Assert
        BusinessException exception = assertThrows(BusinessException.class, () -> {
            userService.deleteAccount(userId.toString(), email, wrongConfirmation);
        });
        
        assertEquals("Invalid confirmation string", exception.getMessage());
        verify(userRepository, times(1)).findById(userId);
        verify(userRepository, never()).deleteById(any());
    }
}
