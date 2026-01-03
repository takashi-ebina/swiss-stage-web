package com.swiss_stage.unit.domain.participant;

import com.swiss_stage.domain.participant.Rank;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Rank値オブジェクトの単体テスト 段級位のパースと比較機能を検証
 */
class RankTest {

    @ParameterizedTest(name = "正常系: {0} -> level={1}, displayName={2}")
    @CsvSource({
            "初段, 1, 初段",
            "2段, 2, 2段",
            "3段, 3, 3段",
            "5段, 5, 5段",
            "9段, 9, 9段",
            "1級, -1, 1級",
            "3級, -3, 3級",
            "10級, -10, 10級",
            "20級, -20, 20級"
    })
    void testParse_ValidFormats(String input, int expectedLevel, String expectedDisplayName) {
        // Act
        Rank rank = Rank.parse(input);

        // Assert
        assertEquals(expectedLevel, rank.level(), "level値が正しいこと");
        assertEquals(expectedDisplayName, rank.displayName(), "displayName値が正しいこと");
    }

    @ParameterizedTest(name = "異常系: {0}は不正なフォーマット")
    @ValueSource(strings = {
            "abc段",
            "0段",
            "1段", // 初段のみ有効
            "10段",
            "100段",
            "0級",
            "21級",
            "100級",
            "初級", // 仕様変更により削除
            "段",
            "級",
            "3",
            "三段",
            "3Dan"
    })
    void testParse_InvalidFormats(String input) {
        // Act & Assert
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> Rank.parse(input),
                "不正なフォーマットでIllegalArgumentExceptionがスローされること");
        assertTrue(exception.getMessage().contains("段級位の形式が不正です"),
                "エラーメッセージに'段級位の形式が不正です'が含まれること");
    }

    @ParameterizedTest(name = "異常系: nullまたは空文字")
    @NullAndEmptySource
    void testParse_NullOrEmpty(String input) {
        // Act & Assert
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> Rank.parse(input),
                "nullまたは空文字でIllegalArgumentExceptionがスローされること");
    }

    @Test
    void testCompareTo_DescendingOrder() {
        // Arrange
        Rank rank9Dan = Rank.parse("9段");
        Rank rank5Dan = Rank.parse("5段");
        Rank rank初段 = Rank.parse("初段");
        Rank rank1Kyu = Rank.parse("1級");
        Rank rank3Kyu = Rank.parse("3級");
        Rank rank20Kyu = Rank.parse("20級");

        // Assert: 降順（高段位が先）
        assertTrue(rank9Dan.compareTo(rank5Dan) < 0, "9段 > 5段");
        assertTrue(rank5Dan.compareTo(rank初段) < 0, "5段 > 初段");
        assertTrue(rank初段.compareTo(rank1Kyu) < 0, "初段 > 1級");
        assertTrue(rank1Kyu.compareTo(rank3Kyu) < 0, "1級 > 3級");
        assertTrue(rank3Kyu.compareTo(rank20Kyu) < 0, "3級 > 20級");
    }

    @Test
    void testCompareTo_SameRank() {
        // Arrange
        Rank rank1 = Rank.parse("3段");
        Rank rank2 = Rank.parse("3段");

        // Assert
        assertEquals(0, rank1.compareTo(rank2), "同じ段級位は等しい");
    }

    @Test
    void testEquals_SameValue() {
        // Arrange
        Rank rank1 = Rank.parse("5段");
        Rank rank2 = Rank.parse("5段");

        // Assert
        assertEquals(rank1, rank2, "同じ値のRankは等価");
        assertEquals(rank1.hashCode(), rank2.hashCode(), "hashCodeも等しい");
    }

    @Test
    void testEquals_DifferentValue() {
        // Arrange
        Rank rank1 = Rank.parse("5段");
        Rank rank2 = Rank.parse("3段");

        // Assert
        assertNotEquals(rank1, rank2, "異なる値のRankは非等価");
    }

    @Test
    void testToString() {
        // Arrange
        Rank rank = Rank.parse("3段");

        // Assert
        String result = rank.toString();
        assertTrue(result.contains("3") && result.contains("3段"),
                "toString()にlevelとdisplayNameが含まれること");
    }

    @ParameterizedTest(name = "境界値テスト: {0}")
    @CsvSource({
            "初段, 1",
            "9段, 9",
            "1級, -1",
            "20級, -20"
    })
    void testParse_BoundaryValues(String input, int expectedLevel) {
        // Act
        Rank rank = Rank.parse(input);

        // Assert
        assertEquals(expectedLevel, rank.level(), "境界値が正しくパースされること");
    }
}
