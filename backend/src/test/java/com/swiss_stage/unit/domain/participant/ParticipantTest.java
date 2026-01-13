package com.swiss_stage.unit.domain.participant;

import com.swiss_stage.domain.participant.Participant;
import com.swiss_stage.domain.participant.Rank;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Participant エンティティの単体テスト
 */
class ParticipantTest {

        @Test
        void testConstructor_ValidParticipant() {
                // Arrange
                UUID participantId = UUID.randomUUID();
                UUID groupId = UUID.randomUUID();
                String affiliation = "株式会社ABC";
                String name = "山田太郎";
                Rank rank = Rank.parse("3段");
                int registrationOrder = 1;

                // Act
                Participant participant = new Participant(
                                participantId, groupId, affiliation, name, rank, false,
                                registrationOrder);

                // Assert
                assertEquals(participantId, participant.participantId());
                assertEquals(groupId, participant.groupId());
                assertEquals(affiliation, participant.affiliation());
                assertEquals(name, participant.name());
                assertEquals(rank, participant.rank());
                assertFalse(participant.isDummy());
                assertEquals(registrationOrder, participant.registrationOrder());
        }

        @Test
        void testConstructor_WithNullAffiliation() {
                // Arrange
                UUID participantId = UUID.randomUUID();
                UUID groupId = UUID.randomUUID();
                String name = "山田太郎";
                Rank rank = Rank.parse("3段");

                // Act
                Participant participant = new Participant(
                                participantId, groupId, null, name, rank, false, 1);

                // Assert
                assertNull(participant.affiliation(), "affiliationはnull許容");
        }

        @Test
        void testConstructor_DummyParticipant() {
                // Arrange
                UUID participantId = UUID.randomUUID();
                UUID groupId = UUID.randomUUID();
                String name = "ダミーユーザー（不戦勝）";

                // Act
                Participant participant = new Participant(
                                participantId, groupId, null, name, Rank.parse("20級"), true, 1);

                // Assert
                assertTrue(participant.isDummy(), "isDummyがtrueであること");
                assertEquals(Rank.parse("20級"), participant.rank(), "ダミーユーザーのrankは20級");
                assertNull(participant.affiliation());
        }

        @Test
        void testConstructor_NullParticipantId() {
                // Act & Assert
                assertThrows(
                                NullPointerException.class,
                                () -> new Participant(null, UUID.randomUUID(), null, "名前",
                                                Rank.parse("3段"), false,
                                                1));
        }

        @Test
        void testConstructor_NullGroupId() {
                // Act & Assert
                assertThrows(
                                NullPointerException.class,
                                () -> new Participant(UUID.randomUUID(), null, null, "名前",
                                                Rank.parse("3段"), false,
                                                1));
        }

        @Test
        void testConstructor_NullName() {
                // Act & Assert
                NullPointerException exception = assertThrows(
                                NullPointerException.class,
                                () -> new Participant(UUID.randomUUID(), UUID.randomUUID(), null,
                                                null,
                                                Rank.parse("3段"), false, 1));
                assertTrue(exception.getMessage().contains("nameは必須です"));
        }

        @Test
        void testConstructor_EmptyName() {
                // Act & Assert
                IllegalArgumentException exception = assertThrows(
                                IllegalArgumentException.class,
                                () -> new Participant(UUID.randomUUID(), UUID.randomUUID(), null,
                                                "",
                                                Rank.parse("3段"), false, 1));
                assertTrue(exception.getMessage().contains("nameは空白のみにできません"));
        }

        @Test
        void testConstructor_BlankName() {
                // Act & Assert
                IllegalArgumentException exception = assertThrows(
                                IllegalArgumentException.class,
                                () -> new Participant(UUID.randomUUID(), UUID.randomUUID(), null,
                                                "   ",
                                                Rank.parse("3段"), false, 1));
                assertTrue(exception.getMessage().contains("nameは空白のみにできません"));
        }

        @ParameterizedTest(name = "name長さ={0}文字")
        @ValueSource(ints = {1, 10, 25, 49, 50})
        void testConstructor_ValidNameLength(int length) {
                // Arrange
                String name = "あ".repeat(length);

                // Act
                Participant participant = new Participant(
                                UUID.randomUUID(), UUID.randomUUID(), null, name, Rank.parse("3段"),
                                false, 1);

                // Assert
                assertEquals(name, participant.name());
        }

        @Test
        void testConstructor_NameTooLong() {
                // Arrange
                String name = "あ".repeat(51);

                // Act & Assert
                IllegalArgumentException exception = assertThrows(
                                IllegalArgumentException.class,
                                () -> new Participant(UUID.randomUUID(), UUID.randomUUID(), null,
                                                name,
                                                Rank.parse("3段"), false, 1));
                assertTrue(exception.getMessage().contains("50文字以内"));
        }

        @ParameterizedTest(name = "affiliation長さ={0}文字")
        @ValueSource(ints = {1, 50, 99, 100})
        void testConstructor_ValidAffiliationLength(int length) {
                // Arrange
                String affiliation = "あ".repeat(length);

                // Act
                Participant participant = new Participant(
                                UUID.randomUUID(), UUID.randomUUID(), affiliation, "名前",
                                Rank.parse("3段"), false,
                                1);

                // Assert
                assertEquals(affiliation, participant.affiliation());
        }

        @Test
        void testConstructor_AffiliationTooLong() {
                // Arrange
                String affiliation = "あ".repeat(101);

                // Act & Assert
                IllegalArgumentException exception = assertThrows(
                                IllegalArgumentException.class,
                                () -> new Participant(UUID.randomUUID(), UUID.randomUUID(),
                                                affiliation, "名前",
                                                Rank.parse("3段"), false, 1));
                assertTrue(exception.getMessage().contains("100文字以内"));
        }

        @Test
        void testConstructor_NormalParticipantWithNullRank() {
                // Act & Assert
                NullPointerException exception = assertThrows(
                                NullPointerException.class,
                                () -> new Participant(UUID.randomUUID(), UUID.randomUUID(), null,
                                                "名前", null, false,
                                                1));
                assertTrue(exception.getMessage().contains("rankは必須です"));
        }

        @Test
        void testEquals_SameValues() {
                // Arrange
                UUID participantId = UUID.randomUUID();
                UUID groupId = UUID.randomUUID();
                Participant p1 =
                                new Participant(participantId, groupId, null, "名前",
                                                Rank.parse("3段"), false, 1);
                Participant p2 =
                                new Participant(participantId, groupId, null, "名前",
                                                Rank.parse("3段"), false, 1);

                // Assert
                assertEquals(p1, p2);
                assertEquals(p1.hashCode(), p2.hashCode());
        }

        @Test
        void testEquals_DifferentParticipantId() {
                // Arrange
                UUID groupId = UUID.randomUUID();
                Participant p1 =
                                new Participant(UUID.randomUUID(), groupId, null, "名前",
                                                Rank.parse("3段"), false, 1);
                Participant p2 =
                                new Participant(UUID.randomUUID(), groupId, null, "名前",
                                                Rank.parse("3段"), false, 1);

                // Assert
                assertNotEquals(p1, p2);
        }

        @Test
        void testToString() {
                // Arrange
                Participant participant = new Participant(
                                UUID.randomUUID(), UUID.randomUUID(), "株式会社ABC", "山田太郎",
                                Rank.parse("3段"), false,
                                1);

                // Assert
                String result = participant.toString();
                assertTrue(result.contains("山田太郎"));
                assertTrue(result.contains("3段"));
        }

        @Test
        void testConstructor_NullRank() {
                // Act & Assert
                NullPointerException exception = assertThrows(
                                NullPointerException.class,
                                () -> new Participant(UUID.randomUUID(), UUID.randomUUID(), null,
                                                "名前", null, false, 1));
                assertTrue(exception.getMessage().contains("rankは必須です"));
        }
}
