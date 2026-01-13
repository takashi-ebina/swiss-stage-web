package com.swiss_stage.unit.infrastructure.dynamodb;

import com.swiss_stage.domain.group.Group;
import com.swiss_stage.infrastructure.dynamodb.DynamoDBGroupRepository;
import com.swiss_stage.infrastructure.dynamodb.entity.GroupEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import org.mockito.ArgumentMatchers;

/**
 * DynamoDBGroupRepositoryの単体テスト DynamoDBクライアントをモック化して動作を検証
 */
@ExtendWith(MockitoExtension.class)

class DynamoDBGroupRepositoryTest {

    @Mock
    private DynamoDbEnhancedClient enhancedClient;

    @Mock
    private DynamoDbTable<GroupEntity> groupTable;

    private DynamoDBGroupRepository repository;

    @BeforeEach
    void setUp() {
        when(enhancedClient.table(eq("Group"), ArgumentMatchers.<TableSchema<GroupEntity>>any()))
                .thenReturn(groupTable);
        repository = new DynamoDBGroupRepository(enhancedClient);
    }

    @Test
    void testFindByTournamentIdAndGroupNumber_Found() {
        // Arrange
        UUID groupId = UUID.randomUUID();
        UUID tournamentId = UUID.randomUUID();
        GroupEntity entity = new GroupEntity();
        entity.setGroupId(groupId.toString());
        entity.setTournamentId(tournamentId.toString());
        entity.setGroupNumber(1);
        entity.setDisplayName("GROUP 1");

        when(groupTable.getItem(any(Key.class))).thenReturn(entity);

        // Act
        Optional<Group> result = repository.findByTournamentIdAndGroupNumber(tournamentId, 1);

        // Assert
        assertTrue(result.isPresent());
        assertEquals(groupId, result.get().groupId());
        assertEquals(1, result.get().groupNumber());
    }

    @Test
    void testFindByTournamentIdAndGroupNumber_NotFound() {
        // Arrange
        UUID tournamentId = UUID.randomUUID();
        when(groupTable.getItem(any(Key.class))).thenReturn(null);

        // Act
        Optional<Group> result = repository.findByTournamentIdAndGroupNumber(tournamentId, 1);

        // Assert
        assertFalse(result.isPresent());
    }

    @Test
    void testSave() {
        // Arrange
        UUID groupId = UUID.randomUUID();
        UUID tournamentId = UUID.randomUUID();
        Group group = new Group(groupId, tournamentId, 3);

        // putItemはvoidなのでモック不要（デフォルトで何もしない）

        // Act
        Group result = repository.save(group);

        // Assert
        assertNotNull(result);
        assertEquals(groupId, result.groupId());
        assertEquals(3, result.groupNumber());
        verify(groupTable, times(1)).putItem(any(GroupEntity.class));
    }

    @Test
    void testDelete() {
        // Arrange
        UUID tournamentId = UUID.randomUUID();
        when(groupTable.deleteItem(any(Key.class))).thenReturn(null);

        // Act
        repository.delete(tournamentId, 3);

        // Assert
        verify(groupTable, times(1)).deleteItem(any(Key.class));
    }
}
