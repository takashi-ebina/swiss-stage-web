package com.swiss_stage.infrastructure.dynamodb.entity;

import com.swiss_stage.domain.group.Group;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbSortKey;

import java.util.UUID;

/**
 * DynamoDB Group テーブルのエンティティ
 * 
 * テーブル設計: - PK: tournamentId - SK: groupNumber
 */
@DynamoDbBean
public class GroupEntity {

    private String groupId;
    private String tournamentId;
    private Integer groupNumber;
    private String displayName;

    /**
     * ドメインエンティティからDynamoDBエンティティに変換
     * 
     * @param group ドメインエンティティ
     * @return DynamoDBエンティティ
     */
    public static GroupEntity fromDomain(Group group) {
        GroupEntity entity = new GroupEntity();
        entity.setGroupId(group.getGroupId().toString());
        entity.setTournamentId(group.getTournamentId().toString());
        entity.setGroupNumber(group.getGroupNumber());
        entity.setDisplayName(group.getDisplayName());
        return entity;
    }

    /**
     * DynamoDBエンティティからドメインエンティティに変換
     * 
     * @return ドメインエンティティ
     */
    public Group toDomain() {
        return new Group(
                UUID.fromString(groupId),
                UUID.fromString(tournamentId),
                groupNumber);
    }

    // Getters and Setters

    public String getGroupId() {
        return groupId;
    }

    public void setGroupId(String groupId) {
        this.groupId = groupId;
    }

    @DynamoDbPartitionKey
    public String getTournamentId() {
        return tournamentId;
    }

    public void setTournamentId(String tournamentId) {
        this.tournamentId = tournamentId;
    }

    @DynamoDbSortKey
    public Integer getGroupNumber() {
        return groupNumber;
    }

    public void setGroupNumber(Integer groupNumber) {
        this.groupNumber = groupNumber;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }
}
