package com.swiss_stage.unit.application;

import com.swiss_stage.application.service.UserService;
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
        User result = userService.findOrCreateUser(googleId, email, displayName);

        // Assert
        assertNotNull(result);
        assertEquals(googleId, result.getGoogleId());
        assertEquals(email, result.getEmail());
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
        User result = userService.findOrCreateUser(googleId, email, displayName);

        // Assert
        assertNotNull(result);
        assertEquals(googleId, result.getGoogleId());
        assertEquals(email, result.getEmail());
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
        Optional<User> result = userService.findById(userId);

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
        Optional<User> result = userService.findById(userId);

        // Assert
        assertFalse(result.isPresent());
        verify(userRepository, times(1)).findById(userId);
    }
}
