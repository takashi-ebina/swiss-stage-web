package com.swiss_stage.unit.domain.participant;

import com.swiss_stage.domain.common.DomainException;
import com.swiss_stage.domain.group.Group;
import com.swiss_stage.domain.participant.GroupParticipantList;
import com.swiss_stage.domain.participant.Participant;
import com.swiss_stage.domain.participant.Rank;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * GroupParticipantList 集約ルートの単体テスト
 */
class GroupParticipantListTest {

    private Group createTestGroup() {
        return new Group(UUID.randomUUID(), UUID.randomUUID(), 1);
    }

    private Participant createTestParticipant(UUID groupId, String name, String rankStr,
            int order) {
        return new Participant(
                UUID.randomUUID(),
                groupId,
                null,
                name,
                Rank.parse(rankStr),
                false,
                order);
    }

    @Test
    void testConstructor_EmptyList() {
        // Arrange
        Group group = createTestGroup();

        // Act
        GroupParticipantList list = new GroupParticipantList(group);

        // Assert
        assertEquals(0, list.getParticipantCount(), "初期状態は0名");
        assertTrue(list.getParticipants().isEmpty(), "参加者リストが空");
    }

    @Test
    void testAddParticipant_FirstParticipant() {
        // Arrange
        Group group = createTestGroup();
        GroupParticipantList list = new GroupParticipantList(group);
        Participant participant = createTestParticipant(group.groupId(), "山田太郎", "3段", 1);

        // Act
        list.addParticipant(participant);

        // Assert
        assertEquals(2, list.getParticipantCount(), "1名追加後、ダミーが追加されて2名");
        List<Participant> participants = list.getParticipants();
        assertEquals(1, participants.stream().filter(p -> !p.isDummy()).count(), "実参加者は1名");
        assertEquals(1, participants.stream().filter(Participant::isDummy).count(), "ダミーユーザーが1名");
    }

    @Test
    void testAddParticipant_SecondParticipant() {
        // Arrange
        Group group = createTestGroup();
        GroupParticipantList list = new GroupParticipantList(group);
        list.addParticipant(createTestParticipant(group.groupId(), "山田太郎", "3段", 1));

        // Act
        list.addParticipant(createTestParticipant(group.groupId(), "鈴木次郎", "5段", 2));

        // Assert
        assertEquals(2, list.getParticipantCount(), "2名追加後、ダミーが削除されて2名");
        assertEquals(2, list.getParticipants().stream().filter(p -> !p.isDummy()).count(),
                "実参加者は2名");
        assertEquals(0, list.getParticipants().stream().filter(Participant::isDummy).count(),
                "ダミーユーザーは0名");
    }

    @Test
    void testAddParticipant_MaxCapacity() {
        // Arrange
        Group group = createTestGroup();
        GroupParticipantList list = new GroupParticipantList(group);

        // 32名追加
        for (int i = 1; i <= 32; i++) {
            list.addParticipant(createTestParticipant(group.groupId(), "参加者" + i, "3段", i));
        }

        // Assert
        assertEquals(32, list.getParticipantCount(), "32名まで追加可能");
    }

    @Test
    void testAddParticipant_ExceedMaxCapacity() {
        // Arrange
        Group group = createTestGroup();
        GroupParticipantList list = new GroupParticipantList(group);

        // 32名追加
        for (int i = 1; i <= 32; i++) {
            list.addParticipant(createTestParticipant(group.groupId(), "参加者" + i, "3段", i));
        }

        // Act & Assert
        DomainException exception = assertThrows(
                DomainException.class,
                () -> list.addParticipant(
                        createTestParticipant(group.groupId(), "参加者33", "3段", 33)),
                "33名目の追加でDomainExceptionがスローされること");
        assertTrue(exception.getMessage().contains("32名"), "エラーメッセージに上限情報が含まれること");
    }

    @Test
    void testRemoveParticipant_FromTwoParticipants() {
        // Arrange
        Group group = createTestGroup();
        GroupParticipantList list = new GroupParticipantList(group);
        Participant p1 = createTestParticipant(group.groupId(), "山田太郎", "3段", 1);
        Participant p2 = createTestParticipant(group.groupId(), "鈴木次郎", "5段", 2);
        list.addParticipant(p1);
        list.addParticipant(p2);

        // Act
        list.removeParticipant(p1.participantId());

        // Assert
        assertEquals(2, list.getParticipantCount(), "1名削除後、ダミーが追加されて2名");
        assertEquals(1, list.getParticipants().stream().filter(p -> !p.isDummy()).count(),
                "実参加者は1名");
        assertEquals(1, list.getParticipants().stream().filter(Participant::isDummy).count(),
                "ダミーユーザーが1名");
    }

    @Test
    void testRemoveParticipant_FromThreeParticipants() {
        // Arrange
        Group group = createTestGroup();
        GroupParticipantList list = new GroupParticipantList(group);
        Participant p1 = createTestParticipant(group.groupId(), "山田太郎", "3段", 1);
        Participant p2 = createTestParticipant(group.groupId(), "鈴木次郎", "5段", 2);
        Participant p3 = createTestParticipant(group.groupId(), "佐藤三郎", "4段", 3);
        list.addParticipant(p1);
        list.addParticipant(p2);
        list.addParticipant(p3);

        // Act
        list.removeParticipant(p1.participantId());

        // Assert
        assertEquals(2, list.getParticipantCount(), "1名削除後、ダミーが削除されて2名");
        assertEquals(2, list.getParticipants().stream().filter(p -> !p.isDummy()).count(),
                "実参加者は2名");
        assertEquals(0, list.getParticipants().stream().filter(Participant::isDummy).count(),
                "ダミーユーザーは0名");
    }

    @Test
    void testGetSortedParticipants_ByRankDescending() {
        // Arrange
        Group group = createTestGroup();
        GroupParticipantList list = new GroupParticipantList(group);
        list.addParticipant(createTestParticipant(group.groupId(), "山田太郎", "3段", 1));
        list.addParticipant(createTestParticipant(group.groupId(), "鈴木次郎", "5段", 2));
        list.addParticipant(createTestParticipant(group.groupId(), "佐藤三郎", "初段", 3));
        list.addParticipant(createTestParticipant(group.groupId(), "田中四郎", "2級", 4));

        // Act
        List<Participant> sorted = list.getSortedParticipants();

        // Assert
        assertEquals("鈴木次郎", sorted.get(0).name(), "1位は5段");
        assertEquals("山田太郎", sorted.get(1).name(), "2位は3段");
        assertEquals("佐藤三郎", sorted.get(2).name(), "3位は初段");
        assertEquals("田中四郎", sorted.get(3).name(), "4位は2級");
    }

    @Test
    void testGetSortedParticipants_SameRankByRegistrationOrder() {
        // Arrange
        Group group = createTestGroup();
        GroupParticipantList list = new GroupParticipantList(group);
        list.addParticipant(createTestParticipant(group.groupId(), "山田太郎", "3段", 1));
        list.addParticipant(createTestParticipant(group.groupId(), "鈴木次郎", "3段", 2));
        list.addParticipant(createTestParticipant(group.groupId(), "佐藤三郎", "3段", 3));

        // Act
        List<Participant> sorted = list.getSortedParticipants();

        // Assert
        assertEquals("山田太郎", sorted.get(0).name(), "同段級位は登録順（山田が先）");
        assertEquals("鈴木次郎", sorted.get(1).name(), "同段級位は登録順（鈴木が次）");
        assertEquals("佐藤三郎", sorted.get(2).name(), "同段級位は登録順（佐藤が最後）");
    }

    @Test
    void testGetSortedParticipants_DummyAtEnd() {
        // Arrange
        Group group = createTestGroup();
        GroupParticipantList list = new GroupParticipantList(group);
        list.addParticipant(createTestParticipant(group.groupId(), "山田太郎", "3段", 1));
        list.addParticipant(createTestParticipant(group.groupId(), "鈴木次郎", "5段", 2));
        list.addParticipant(createTestParticipant(group.groupId(), "佐藤三郎", "初段", 3));

        // Act
        List<Participant> sorted = list.getSortedParticipants();

        // Assert
        Participant last = sorted.get(sorted.size() - 1);
        assertTrue(last.isDummy(), "ダミーユーザーが末尾に配置されること");
    }

    @Test
    void testExportToCsv_ExcludesDummy() {
        // Arrange
        Group group = createTestGroup();
        GroupParticipantList list = new GroupParticipantList(group);
        list.addParticipant(createTestParticipant(group.groupId(), "山田太郎", "3段", 1));
        list.addParticipant(createTestParticipant(group.groupId(), "鈴木次郎", "5段", 2));
        list.addParticipant(createTestParticipant(group.groupId(), "佐藤三郎", "初段", 3));

        // Act
        List<Participant> exported = list.exportToCsv();

        // Assert
        assertEquals(3, exported.size(), "ダミーユーザーを除く3名が出力される");
        assertTrue(exported.stream().noneMatch(Participant::isDummy), "ダミーユーザーが含まれないこと");
    }

    @Test
    void testEnsureEvenCount_ZeroParticipants() {
        // Arrange
        Group group = createTestGroup();
        GroupParticipantList list = new GroupParticipantList(group);

        // Assert
        assertEquals(0, list.getParticipantCount(), "0名の場合はダミー追加なし");
    }

    @Test
    void testEnsureEvenCount_OneParticipant() {
        // Arrange
        Group group = createTestGroup();
        GroupParticipantList list = new GroupParticipantList(group);
        list.addParticipant(createTestParticipant(group.groupId(), "山田太郎", "3段", 1));

        // Assert
        assertEquals(2, list.getParticipantCount(), "1名の場合はダミーが追加されて2名");
    }

    @Test
    void testCanAddParticipants_BelowLimit() {
        // Arrange
        Group group = createTestGroup();
        GroupParticipantList list = new GroupParticipantList(group);
        for (int i = 1; i <= 30; i++) {
            list.addParticipant(createTestParticipant(group.groupId(), "参加者" + i, "3段", i));
        }

        // Assert
        assertTrue(list.canAddParticipants(2), "残り2名追加可能");
        assertFalse(list.canAddParticipants(3), "3名は追加不可");
    }

    @Test
    void testGetRealParticipantCount() {
        // Arrange
        Group group = createTestGroup();
        GroupParticipantList list = new GroupParticipantList(group);
        list.addParticipant(createTestParticipant(group.groupId(), "山田太郎", "3段", 1));
        list.addParticipant(createTestParticipant(group.groupId(), "鈴木次郎", "5段", 2));
        list.addParticipant(createTestParticipant(group.groupId(), "佐藤三郎", "初段", 3));

        // Assert
        assertEquals(3, list.getRealParticipantCount(), "実参加者は3名");
        assertEquals(4, list.getParticipantCount(), "ダミー含めて4名");
    }
}
