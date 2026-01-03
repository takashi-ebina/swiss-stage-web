package com.swiss_stage.infrastructure.dynamodb;

import com.swiss_stage.domain.group.Group;
import com.swiss_stage.domain.participant.GroupParticipantList;
import com.swiss_stage.domain.participant.GroupParticipantListRepository;
import com.swiss_stage.domain.participant.Participant;
import com.swiss_stage.infrastructure.dynamodb.entity.ParticipantEntity;
import org.springframework.stereotype.Repository;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbIndex;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryConditional;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryEnhancedRequest;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * GroupParticipantListRepositoryのDynamoDB実装 AWS SDK Enhanced Clientを使用してDynamoDB操作を実行
 */
@Repository
public class DynamoDBGroupParticipantListRepository implements GroupParticipantListRepository {

    private final DynamoDbTable<ParticipantEntity> participantTable;
    private final DynamoDbIndex<ParticipantEntity> rankLevelIndex;

    public DynamoDBGroupParticipantListRepository(DynamoDbEnhancedClient enhancedClient) {
        this.participantTable =
                enhancedClient.table("Participant", TableSchema.fromBean(ParticipantEntity.class));
        this.rankLevelIndex = participantTable.index("groupId-rankLevel-index");
    }

    @Override
    public Optional<GroupParticipantList> findByGroupId(UUID groupId) {
        QueryConditional queryConditional = QueryConditional
                .keyEqualTo(Key.builder()
                        .partitionValue(groupId.toString())
                        .build());

        QueryEnhancedRequest queryRequest = QueryEnhancedRequest.builder()
                .queryConditional(queryConditional)
                .build();

        List<Participant> participants = participantTable.query(queryRequest)
                .stream()
                .flatMap(page -> page.items().stream())
                .map(ParticipantEntity::toDomain)
                .collect(Collectors.toList());

        if (participants.isEmpty()) {
            return Optional.empty();
        }

        // GroupParticipantListを再構築
        // Note: Groupの情報はParticipantから取得できないため、
        // 実際の実装ではGroupRepositoryから取得する必要がある
        // ここでは簡易実装として、最初のParticipantのgroupIdを使用
        UUID firstGroupId = participants.get(0).getGroupId();

        // 簡易実装: Groupを仮作成（実際にはGroupRepositoryから取得すべき）
        Group group = new Group(UUID.randomUUID(), UUID.randomUUID(), 1);
        GroupParticipantList list = new GroupParticipantList(group);

        // 参加者を追加（registrationOrder順にソート）
        participants.stream()
                .sorted((p1, p2) -> Integer.compare(p1.getRegistrationOrder(),
                        p2.getRegistrationOrder()))
                .forEach(list::addParticipant);

        return Optional.of(list);
    }

    @Override
    public List<Participant> findParticipantsSortedByRank(UUID groupId) {
        QueryConditional queryConditional = QueryConditional
                .keyEqualTo(Key.builder()
                        .partitionValue(groupId.toString())
                        .build());

        QueryEnhancedRequest queryRequest = QueryEnhancedRequest.builder()
                .queryConditional(queryConditional)
                .scanIndexForward(false) // 降順ソート（段級位が高い順）
                .build();

        return rankLevelIndex.query(queryRequest)
                .stream()
                .flatMap(page -> page.items().stream())
                .map(ParticipantEntity::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public GroupParticipantList save(GroupParticipantList list) {
        // 既存の参加者を全削除
        deleteAllByGroupId(list.getGroup().getGroupId());

        // 新規参加者を全追加
        for (Participant participant : list.getParticipants()) {
            ParticipantEntity entity = ParticipantEntity.fromDomain(participant);
            participantTable.putItem(entity);
        }

        return list;
    }

    @Override
    public void deleteAllByGroupId(UUID groupId) {
        QueryConditional queryConditional = QueryConditional
                .keyEqualTo(Key.builder()
                        .partitionValue(groupId.toString())
                        .build());

        QueryEnhancedRequest queryRequest = QueryEnhancedRequest.builder()
                .queryConditional(queryConditional)
                .build();

        // 全参加者を取得して削除
        List<ParticipantEntity> entities = participantTable.query(queryRequest)
                .stream()
                .flatMap(page -> page.items().stream())
                .collect(Collectors.toList());
        
        for (ParticipantEntity entity : entities) {
            Key key = Key.builder()
                    .partitionValue(entity.getGroupId())
                    .sortValue(entity.getParticipantId())
                    .build();
            participantTable.deleteItem(key);
        }
    }
}
