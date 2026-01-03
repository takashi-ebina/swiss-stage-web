package com.swiss_stage.infrastructure.dynamodb;

import com.swiss_stage.domain.group.Group;
import com.swiss_stage.domain.group.GroupRepository;
import com.swiss_stage.infrastructure.dynamodb.entity.GroupEntity;
import org.springframework.stereotype.Repository;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
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
 * GroupRepositoryのDynamoDB実装 AWS SDK Enhanced Clientを使用してDynamoDB操作を実行
 */
@Repository
public class DynamoDBGroupRepository implements GroupRepository {

    private final DynamoDbTable<GroupEntity> groupTable;

    public DynamoDBGroupRepository(DynamoDbEnhancedClient enhancedClient) {
        this.groupTable = enhancedClient.table("Group", TableSchema.fromBean(GroupEntity.class));
    }

    @Override
    public Optional<Group> findByTournamentIdAndGroupNumber(UUID tournamentId, int groupNumber) {
        Key key = Key.builder()
                .partitionValue(tournamentId.toString())
                .sortValue(groupNumber)
                .build();

        GroupEntity entity = groupTable.getItem(key);
        return Optional.ofNullable(entity).map(GroupEntity::toDomain);
    }

    @Override
    public List<Group> findByTournamentId(UUID tournamentId) {
        QueryConditional queryConditional = QueryConditional
                .keyEqualTo(Key.builder()
                        .partitionValue(tournamentId.toString())
                        .build());

        QueryEnhancedRequest queryRequest = QueryEnhancedRequest.builder()
                .queryConditional(queryConditional)
                .build();

        return groupTable.query(queryRequest)
                .items()
                .stream()
                .map(GroupEntity::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public Group save(Group group) {
        GroupEntity entity = GroupEntity.fromDomain(group);
        groupTable.putItem(entity);
        return group;
    }

    @Override
    public void delete(UUID tournamentId, int groupNumber) {
        Key key = Key.builder()
                .partitionValue(tournamentId.toString())
                .sortValue(groupNumber)
                .build();

        groupTable.deleteItem(key);
    }
}
