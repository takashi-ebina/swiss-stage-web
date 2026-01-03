package com.swiss_stage.unit.infrastructure.dynamodb.entity;

import com.swiss_stage.domain.participant.Participant;
import com.swiss_stage.domain.participant.Rank;
import com.swiss_stage.infrastructure.dynamodb.entity.ParticipantEntity;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * ParticipantEntity の単体テスト DynamoDB Item ↔ Domain Entity の変換を検証
 */
class ParticipantEntityTest {

    @Test
    void testFromDomain_NormalParticipant() {
        // Arrange
        UUID participantId = UUID.randomUUID();
        UUID groupId = UUID.randomUUID();
        Participant participant = new Participant(
                participantId,
                groupId,
                "株式会社ABC",
                "山田太郎",
                Rank.parse("3段"),
                false,
                1);

        // Act
        ParticipantEntity entity = ParticipantEntity.fromDomain(participant);

        // Assert
        assertEquals(participantId.toString(), entity.getParticipantId());
        assertEquals(groupId.toString(), entity.getGroupId());
        assertEquals("株式会社ABC", entity.getAffiliation());
        assertEquals("山田太郎", entity.getName());
        assertEquals(3, entity.getRankLevel());
        assertEquals("3段", entity.getRankDisplayName());
        assertFalse(entity.getIsDummy());
        assertEquals(1, entity.getRegistrationOrder());
    }

    @Test
    void testFromDomain_DummyParticipant() {
        // Arrange
        UUID participantId = UUID.randomUUID();
        UUID groupId = UUID.randomUUID();
        Participant participant = new Participant(
                participantId,
                groupId,
                null,
                "ダミーユーザー（不戦勝）",
                null,
                true,
                10);

        // Act
        ParticipantEntity entity = ParticipantEntity.fromDomain(participant);

        // Assert
        assertEquals(participantId.toString(), entity.getParticipantId());
        assertEquals(groupId.toString(), entity.getGroupId());
        assertNull(entity.getAffiliation());
        assertEquals("ダミーユーザー（不戦勝）", entity.getName());
        assertNull(entity.getRankLevel());
        assertNull(entity.getRankDisplayName());
        assertTrue(entity.getIsDummy());
        assertEquals(10, entity.getRegistrationOrder());
    }

    @Test
    void testToDomain_NormalParticipant() {
        // Arrange
        UUID participantId = UUID.randomUUID();
        UUID groupId = UUID.randomUUID();
        ParticipantEntity entity = new ParticipantEntity();
        entity.setParticipantId(participantId.toString());
        entity.setGroupId(groupId.toString());
        entity.setAffiliation("株式会社XYZ");
        entity.setName("鈴木次郎");
        entity.setRankLevel(5);
        entity.setRankDisplayName("5段");
        entity.setIsDummy(false);
        entity.setRegistrationOrder(2);

        // Act
        Participant participant = entity.toDomain();

        // Assert
        assertEquals(participantId, participant.getParticipantId());
        assertEquals(groupId, participant.getGroupId());
        assertEquals("株式会社XYZ", participant.getAffiliation());
        assertEquals("鈴木次郎", participant.getName());
        assertEquals(Rank.parse("5段"), participant.getRank());
        assertFalse(participant.isDummy());
        assertEquals(2, participant.getRegistrationOrder());
    }

    @Test
    void testToDomain_DummyParticipant() {
        // Arrange
        UUID participantId = UUID.randomUUID();
        UUID groupId = UUID.randomUUID();
        ParticipantEntity entity = new ParticipantEntity();
        entity.setParticipantId(participantId.toString());
        entity.setGroupId(groupId.toString());
        entity.setName("ダミーユーザー（不戦勝）");
        entity.setIsDummy(true);
        entity.setRegistrationOrder(5);

        // Act
        Participant participant = entity.toDomain();

        // Assert
        assertEquals(participantId, participant.getParticipantId());
        assertEquals(groupId, participant.getGroupId());
        assertNull(participant.getAffiliation());
        assertEquals("ダミーユーザー（不戦勝）", participant.getName());
        assertNull(participant.getRank());
        assertTrue(participant.isDummy());
        assertEquals(5, participant.getRegistrationOrder());
    }

    @Test
    void testRoundTrip_NormalParticipant() {
        // Arrange
        UUID participantId = UUID.randomUUID();
        UUID groupId = UUID.randomUUID();
        Participant original = new Participant(
                participantId,
                groupId,
                "所属テスト",
                "テスト太郎",
                Rank.parse("初段"),
                false,
                3);

        // Act
        ParticipantEntity entity = ParticipantEntity.fromDomain(original);
        Participant converted = entity.toDomain();

        // Assert
        assertEquals(original.getParticipantId(), converted.getParticipantId());
        assertEquals(original.getGroupId(), converted.getGroupId());
        assertEquals(original.getAffiliation(), converted.getAffiliation());
        assertEquals(original.getName(), converted.getName());
        assertEquals(original.getRank(), converted.getRank());
        assertEquals(original.isDummy(), converted.isDummy());
        assertEquals(original.getRegistrationOrder(), converted.getRegistrationOrder());
    }

    @Test
    void testRoundTrip_KyuRank() {
        // Arrange
        UUID participantId = UUID.randomUUID();
        UUID groupId = UUID.randomUUID();
        Participant original = new Participant(
                participantId,
                groupId,
                null,
                "級位テスト",
                Rank.parse("5級"),
                false,
                1);

        // Act
        ParticipantEntity entity = ParticipantEntity.fromDomain(original);
        Participant converted = entity.toDomain();

        // Assert
        assertEquals(-5, entity.getRankLevel(), "5級はlevel=-5");
        assertEquals("5級", entity.getRankDisplayName());
        assertEquals(original.getRank(), converted.getRank());
    }
}
