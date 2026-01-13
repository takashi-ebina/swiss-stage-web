package com.swiss_stage.unit.domain.common;

import com.swiss_stage.domain.common.DomainException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * DomainException の単体テスト
 */
class DomainExceptionTest {

    @Test
    void testConstructor_WithMessage() {
        // Arrange
        String message = "ドメインエラーが発生しました";

        // Act
        DomainException exception = new DomainException(message);

        // Assert
        assertEquals(message, exception.getMessage(), "メッセージが正しく設定されること");
        assertNull(exception.getCause(), "causeはnullであること");
    }

    @Test
    void testConstructor_WithMessageAndCause() {
        // Arrange
        String message = "ドメインエラーが発生しました";
        Throwable cause = new IllegalStateException("原因となる例外");

        // Act
        DomainException exception = new DomainException(message, cause);

        // Assert
        assertEquals(message, exception.getMessage(), "メッセージが正しく設定されること");
        assertEquals(cause, exception.getCause(), "causeが正しく設定されること");
    }

    @Test
    void testConstructor_WithCause() {
        // Arrange
        Throwable cause = new IllegalStateException("原因となる例外");

        // Act
        DomainException exception = new DomainException(cause);

        // Assert
        assertEquals(cause, exception.getCause(), "causeが正しく設定されること");
        assertTrue(exception.getMessage().contains("IllegalStateException"),
                "メッセージにcauseの情報が含まれること");
    }

    @Test
    void testIsRuntimeException() {
        // Arrange
        DomainException exception = new DomainException("テスト");

        // Assert
        assertInstanceOf(RuntimeException.class, exception,
                "DomainExceptionはRuntimeExceptionを継承していること");
    }

    @Test
    void testThrow() {
        // Act & Assert
        assertThrows(DomainException.class,
                () -> {
                    throw new DomainException("エラー");
                },
                "DomainExceptionがスローされること");
    }
}
