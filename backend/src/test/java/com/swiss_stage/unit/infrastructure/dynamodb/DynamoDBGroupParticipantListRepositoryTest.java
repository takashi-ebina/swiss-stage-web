package com.swiss_stage.unit.infrastructure.dynamodb;

import com.swiss_stage.domain.group.Group;
import com.swiss_stage.domain.participant.GroupParticipantList;
import com.swiss_stage.domain.participant.Participant;
import com.swiss_stage.domain.participant.Rank;
import com.swiss_stage.infrastructure.dynamodb.DynamoDBGroupParticipantListRepository;
import com.swiss_stage.infrastructure.dynamodb.entity.ParticipantEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbIndex;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.enhanced.dynamodb.model.Page;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryEnhancedRequest;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * DynamoDBGroupParticipantListRepositoryの単体テスト DynamoDBクライアントをモック化して動作を検証
 */
@ExtendWith(MockitoExtension.class)
class DynamoDBGroupParticipantListRepositoryTest {

    @Mock
    private DynamoDbEnhancedClient enhancedClient;

    @Mock
    private DynamoDbTable<ParticipantEntity> participantTable;

    @Mock
    private DynamoDbIndex<ParticipantEntity> rankLevelIndex;

    private DynamoDBGroupParticipantListRepository repository;

    @BeforeEach
    void setUp() {
        when(enhancedClient.table(eq("Participant"),
                ArgumentMatchers.<TableSchema<ParticipantEntity>>any()))
                        .thenReturn(participantTable);
        when(participantTable.index("groupId-rankLevel-index")).thenReturn(rankLevelIndex);
        repository = new DynamoDBGroupParticipantListRepository(enhancedClient);
    }

    @Test
    void testFindByGroupId_Found() {
        // Arrange
        UUID groupId = UUID.randomUUID();

        ParticipantEntity entity1 = createParticipantEntity(groupId, "参加者1", "3段", 1);
        ParticipantEntity entity2 = createParticipantEntity(groupId, "参加者2", "初段", 2);

        Page<ParticipantEntity> page = Page.builder(ParticipantEntity.class)
                .items(Arrays.asList(entity1, entity2))
                .build();
        when(participantTable.query(any(QueryEnhancedRequest.class)))
                .thenReturn(() -> Collections.singletonList(page).iterator());

        // Act
        Optional<GroupParticipantList> result = repository.findByGroupId(groupId);

        // Assert
        assertTrue(result.isPresent());
        GroupParticipantList list = result.get();
        // Note: GroupはDynamoDBから復元できないため、簡易実装で新しいGroupIdが生成される
        // ここではparticipantsの内容を検証
        assertEquals(2, list.getParticipants().size());
        assertEquals("参加者1", list.getParticipants().get(0).name());
        assertEquals("参加者2", list.getParticipants().get(1).name());
    }

    @Test
    void testFindParticipantsSortedByRank() {
        // Arrange
        UUID groupId = UUID.randomUUID();

        ParticipantEntity entity1 = createParticipantEntity(groupId, "参加者1", "初段", 1);
        ParticipantEntity entity2 = createParticipantEntity(groupId, "参加者2", "5段", 2);
        ParticipantEntity entity3 = createParticipantEntity(groupId, "参加者3", "3段", 3);

        // GSIクエリ結果（段級位降順）
        Page<ParticipantEntity> page = Page.builder(ParticipantEntity.class)
                .items(Arrays.asList(entity2, entity3, entity1))
                .build();
        when(rankLevelIndex.query(any(QueryEnhancedRequest.class)))
                .thenReturn(() -> Collections.singletonList(page).iterator());

        // Act
        List<Participant> result = repository.findParticipantsSortedByRank(groupId);

        // Assert
        assertEquals(3, result.size());
        assertEquals("5段", result.get(0).rank().displayName());
        assertEquals("3段", result.get(1).rank().displayName());
        assertEquals("初段", result.get(2).rank().displayName());
    }

    @Test
    void testSave() {
        // Arrange
        UUID groupId = UUID.randomUUID();
        UUID tournamentId = UUID.randomUUID();
        Group group = new Group(UUID.randomUUID(), tournamentId, 1);
        GroupParticipantList list = new GroupParticipantList(group);

        Participant p1 = new Participant(
                UUID.randomUUID(), groupId, null, "参加者1", Rank.parse("3段"), false, 1);
        list.addParticipant(p1);

        // deleteAllByGroupIdのモック（空のページを返す）
        Page<ParticipantEntity> emptyPage = Page.builder(ParticipantEntity.class)
                .items(Collections.emptyList())
                .build();
        when(participantTable.query(any(QueryEnhancedRequest.class)))
                .thenReturn(() -> Collections.singletonList(emptyPage).iterator());

        // Act
        GroupParticipantList result = repository.save(list);

        // Assert
        assertNotNull(result);
        assertEquals(group.groupId(), result.getGroup().groupId());
        verify(participantTable, atLeastOnce()).putItem(any(ParticipantEntity.class));
    }

    private ParticipantEntity createParticipantEntity(UUID groupId, String name, String rank,
            int registrationOrder) {
        ParticipantEntity entity = new ParticipantEntity();
        entity.setParticipantId(UUID.randomUUID().toString());
        entity.setGroupId(groupId.toString());
        entity.setName(name);
        entity.setRankLevel(Rank.parse(rank).level());
        entity.setRankDisplayName(rank);
        entity.setIsDummy(false);
        entity.setRegistrationOrder(registrationOrder);
        return entity;
    }
}
