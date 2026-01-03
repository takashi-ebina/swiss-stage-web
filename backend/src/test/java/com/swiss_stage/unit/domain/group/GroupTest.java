package com.swiss_stage.unit.domain.group;

import com.swiss_stage.domain.group.Group;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Group エンティティの単体テスト
 */
class GroupTest {

    @Test
    void testConstructor_ValidGroupNumber() {
        // Arrange
        UUID groupId = UUID.randomUUID();
        UUID tournamentId = UUID.randomUUID();
        int groupNumber = 1;

        // Act
        Group group = new Group(groupId, tournamentId, groupNumber);

        // Assert
        assertEquals(groupId, group.getGroupId(), "groupIdが正しく設定されること");
        assertEquals(tournamentId, group.getTournamentId(), "tournamentIdが正しく設定されること");
        assertEquals(groupNumber, group.getGroupNumber(), "groupNumberが正しく設定されること");
        assertEquals("GROUP 1", group.getDisplayName(), "displayNameが正しく生成されること");
    }

    @ParameterizedTest(name = "正常系: groupNumber={0}")
    @ValueSource(ints = {1, 2, 3, 4, 5, 6, 7, 8})
    void testConstructor_AllValidGroupNumbers(int groupNumber) {
        // Arrange
        UUID groupId = UUID.randomUUID();
        UUID tournamentId = UUID.randomUUID();

        // Act
        Group group = new Group(groupId, tournamentId, groupNumber);

        // Assert
        assertEquals(groupNumber, group.getGroupNumber(), "groupNumberが正しく設定されること");
        assertEquals("GROUP " + groupNumber, group.getDisplayName(),
            "displayNameが'GROUP {groupNumber}'形式であること");
    }

    @ParameterizedTest(name = "異常系: groupNumber={0}は範囲外")
    @ValueSource(ints = {0, -1, 9, 10, 100})
    void testConstructor_InvalidGroupNumber(int groupNumber) {
        // Arrange
        UUID groupId = UUID.randomUUID();
        UUID tournamentId = UUID.randomUUID();

        // Act & Assert
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> new Group(groupId, tournamentId, groupNumber),
            "groupNumberが範囲外でIllegalArgumentExceptionがスローされること"
        );
        assertTrue(exception.getMessage().contains("1〜8"),
            "エラーメッセージに範囲情報が含まれること");
    }

    @Test
    void testConstructor_NullGroupId() {
        // Arrange
        UUID tournamentId = UUID.randomUUID();
        int groupNumber = 1;

        // Act & Assert
        assertThrows(
            NullPointerException.class,
            () -> new Group(null, tournamentId, groupNumber),
            "groupIdがnullでNullPointerExceptionがスローされること"
        );
    }

    @Test
    void testConstructor_NullTournamentId() {
        // Arrange
        UUID groupId = UUID.randomUUID();
        int groupNumber = 1;

        // Act & Assert
        assertThrows(
            NullPointerException.class,
            () -> new Group(groupId, null, groupNumber),
            "tournamentIdがnullでNullPointerExceptionがスローされること"
        );
    }

    @Test
    void testConstants() {
        // Assert
        assertEquals(32, Group.MAX_PARTICIPANTS, "MAX_PARTICIPANTSは32");
        assertEquals(8, Group.MAX_GROUPS, "MAX_GROUPSは8");
    }

    @Test
    void testEquals_SameValues() {
        // Arrange
        UUID groupId = UUID.randomUUID();
        UUID tournamentId = UUID.randomUUID();
        Group group1 = new Group(groupId, tournamentId, 1);
        Group group2 = new Group(groupId, tournamentId, 1);

        // Assert
        assertEquals(group1, group2, "同じ値のGroupは等価");
        assertEquals(group1.hashCode(), group2.hashCode(), "hashCodeも等しい");
    }

    @Test
    void testEquals_DifferentGroupId() {
        // Arrange
        UUID tournamentId = UUID.randomUUID();
        Group group1 = new Group(UUID.randomUUID(), tournamentId, 1);
        Group group2 = new Group(UUID.randomUUID(), tournamentId, 1);

        // Assert
        assertNotEquals(group1, group2, "groupIdが異なるGroupは非等価");
    }

    @Test
    void testToString() {
        // Arrange
        UUID groupId = UUID.randomUUID();
        UUID tournamentId = UUID.randomUUID();
        Group group = new Group(groupId, tournamentId, 1);

        // Assert
        String result = group.toString();
        assertTrue(result.contains("GROUP 1"), "toString()にdisplayNameが含まれること");
        assertTrue(result.contains(groupId.toString()), "toString()にgroupIdが含まれること");
    }
}
