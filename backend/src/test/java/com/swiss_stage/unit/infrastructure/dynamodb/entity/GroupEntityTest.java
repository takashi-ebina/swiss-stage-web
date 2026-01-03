package com.swiss_stage.unit.infrastructure.dynamodb.entity;

import com.swiss_stage.domain.group.Group;
import com.swiss_stage.infrastructure.dynamodb.entity.GroupEntity;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * GroupEntity の単体テスト DynamoDB Item ↔ Domain Entity の変換を検証
 */
class GroupEntityTest {

    @Test
    void testFromDomain() {
        // Arrange
        UUID groupId = UUID.randomUUID();
        UUID tournamentId = UUID.randomUUID();
        Group group = new Group(groupId, tournamentId, 1);

        // Act
        GroupEntity entity = GroupEntity.fromDomain(group);

        // Assert
        assertEquals(groupId.toString(), entity.getGroupId());
        assertEquals(tournamentId.toString(), entity.getTournamentId());
        assertEquals(1, entity.getGroupNumber());
        assertEquals("GROUP 1", entity.getDisplayName());
    }

    @Test
    void testToDomain() {
        // Arrange
        UUID groupId = UUID.randomUUID();
        UUID tournamentId = UUID.randomUUID();
        GroupEntity entity = new GroupEntity();
        entity.setGroupId(groupId.toString());
        entity.setTournamentId(tournamentId.toString());
        entity.setGroupNumber(5);
        entity.setDisplayName("GROUP 5");

        // Act
        Group group = entity.toDomain();

        // Assert
        assertEquals(groupId, group.getGroupId());
        assertEquals(tournamentId, group.getTournamentId());
        assertEquals(5, group.getGroupNumber());
        assertEquals("GROUP 5", group.getDisplayName());
    }

    @Test
    void testRoundTrip() {
        // Arrange
        UUID groupId = UUID.randomUUID();
        UUID tournamentId = UUID.randomUUID();
        Group originalGroup = new Group(groupId, tournamentId, 3);

        // Act
        GroupEntity entity = GroupEntity.fromDomain(originalGroup);
        Group convertedGroup = entity.toDomain();

        // Assert
        assertEquals(originalGroup.getGroupId(), convertedGroup.getGroupId());
        assertEquals(originalGroup.getTournamentId(), convertedGroup.getTournamentId());
        assertEquals(originalGroup.getGroupNumber(), convertedGroup.getGroupNumber());
        assertEquals(originalGroup.getDisplayName(), convertedGroup.getDisplayName());
    }

    @Test
    void testGettersAndSetters() {
        // Arrange
        GroupEntity entity = new GroupEntity();
        String groupId = UUID.randomUUID().toString();
        String tournamentId = UUID.randomUUID().toString();

        // Act
        entity.setGroupId(groupId);
        entity.setTournamentId(tournamentId);
        entity.setGroupNumber(8);
        entity.setDisplayName("GROUP 8");

        // Assert
        assertEquals(groupId, entity.getGroupId());
        assertEquals(tournamentId, entity.getTournamentId());
        assertEquals(8, entity.getGroupNumber());
        assertEquals("GROUP 8", entity.getDisplayName());
    }
}
