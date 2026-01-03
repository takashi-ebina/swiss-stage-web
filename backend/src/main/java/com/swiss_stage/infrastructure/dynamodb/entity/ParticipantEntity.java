package com.swiss_stage.infrastructure.dynamodb.entity;

import com.swiss_stage.domain.participant.Participant;
import com.swiss_stage.domain.participant.Rank;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.*;

import java.util.UUID;

/**
 * DynamoDB Participantテーブルのエンティティ
 * 
 * 主キー: - PK: groupId (Group内の参加者を特定) - SK: participantId (一意の参加者ID)
 * 
 * GSI: - groupId-rankLevel-index: 組内でランクレベル順にソート - groupId-registrationOrder-index: 組内で登録順にソート
 */
@DynamoDbBean
public class ParticipantEntity {

    private String participantId;
    private String groupId;
    private String affiliation;
    private String name;
    private Integer rankLevel;
    private String rankDisplayName;
    private Boolean isDummy;
    private Integer registrationOrder;

    /**
     * ドメインエンティティからDynamoDBエンティティに変換
     */
    public static ParticipantEntity fromDomain(Participant participant) {
        ParticipantEntity entity = new ParticipantEntity();
        entity.setParticipantId(participant.getParticipantId().toString());
        entity.setGroupId(participant.getGroupId().toString());
        entity.setAffiliation(participant.getAffiliation());
        entity.setName(participant.getName());

        // Rankの変換: ダミーユーザーはnull
        if (participant.getRank() != null) {
            entity.setRankLevel(participant.getRank().level());
            entity.setRankDisplayName(participant.getRank().displayName());
        } else {
            entity.setRankLevel(null);
            entity.setRankDisplayName(null);
        }

        entity.setIsDummy(participant.isDummy());
        entity.setRegistrationOrder(participant.getRegistrationOrder());

        return entity;
    }

    /**
     * DynamoDBエンティティからドメインエンティティに変換
     */
    public Participant toDomain() {
        Rank rank = null;
        if (rankDisplayName != null) {
            rank = Rank.parse(rankDisplayName);
        }

        return new Participant(
                UUID.fromString(participantId),
                UUID.fromString(groupId),
                affiliation,
                name,
                rank,
                isDummy,
                registrationOrder);
    }

    @DynamoDbPartitionKey
    @DynamoDbAttribute("groupId")
    public String getGroupId() {
        return groupId;
    }

    public void setGroupId(String groupId) {
        this.groupId = groupId;
    }

    @DynamoDbSortKey
    @DynamoDbAttribute("participantId")
    public String getParticipantId() {
        return participantId;
    }

    public void setParticipantId(String participantId) {
        this.participantId = participantId;
    }

    @DynamoDbAttribute("affiliation")
    public String getAffiliation() {
        return affiliation;
    }

    public void setAffiliation(String affiliation) {
        this.affiliation = affiliation;
    }

    @DynamoDbAttribute("name")
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @DynamoDbSecondaryPartitionKey(indexNames = "groupId-rankLevel-index")
    @DynamoDbAttribute("rankLevel")
    public Integer getRankLevel() {
        return rankLevel;
    }

    public void setRankLevel(Integer rankLevel) {
        this.rankLevel = rankLevel;
    }

    @DynamoDbAttribute("rankDisplayName")
    public String getRankDisplayName() {
        return rankDisplayName;
    }

    public void setRankDisplayName(String rankDisplayName) {
        this.rankDisplayName = rankDisplayName;
    }

    @DynamoDbAttribute("isDummy")
    public Boolean getIsDummy() {
        return isDummy;
    }

    public void setIsDummy(Boolean isDummy) {
        this.isDummy = isDummy;
    }

    @DynamoDbSecondaryPartitionKey(indexNames = "groupId-registrationOrder-index")
    @DynamoDbAttribute("registrationOrder")
    public Integer getRegistrationOrder() {
        return registrationOrder;
    }

    public void setRegistrationOrder(Integer registrationOrder) {
        this.registrationOrder = registrationOrder;
    }
}
