package com.swiss_stage.domain.participant;

import com.swiss_stage.domain.common.DomainException;
import com.swiss_stage.domain.group.Group;

import java.util.*;
import java.util.stream.Collectors;

/**
 * グループ参加者リストの集約ルート
 * 
 * グループに所属する参加者のライフサイクルを管理する。
 * 主な責務:
 * - 参加者の追加・削除（32名上限チェック）
 * - ダミーユーザーの自動管理（奇数→ダミー追加、偶数→削除）
 * - 参加者リストの段級位順・登録順ソート
 * - CSV入出力のデータ整合性保証
 */
public class GroupParticipantList {

    private final Group group;
    private final List<Participant> participants;

    /**
     * コンストラクタ
     * 
     * @param group グループエンティティ
     */
    public GroupParticipantList(Group group) {
        this.group = Objects.requireNonNull(group, "groupは必須です");
        this.participants = new ArrayList<>();
    }

    /**
     * 参加者を追加する
     * 32名上限チェックとダミーユーザー管理を実行する
     * 
     * @param participant 追加する参加者
     * @throws DomainException 32名上限を超える場合
     */
    public void addParticipant(Participant participant) {
        Objects.requireNonNull(participant, "participantは必須です");
        
        // 32名上限チェック（ダミーユーザーを除く）
        validateCapacity(1);
        
        // 参加者追加
        participants.add(participant);
        
        // ダミーユーザー調整
        ensureEvenCount();
    }

    /**
     * 参加者を削除する
     * ダミーユーザー管理を実行する
     * 
     * @param participantId 削除する参加者のID
     */
    public void removeParticipant(UUID participantId) {
        Objects.requireNonNull(participantId, "participantIdは必須です");
        
        participants.removeIf(p -> p.getParticipantId().equals(participantId));
        
        // ダミーユーザー調整
        ensureEvenCount();
    }

    /**
     * 参加者数が偶数になるようダミーユーザーを調整する
     * - 奇数の場合: ダミーユーザーを追加
     * - 偶数の場合: ダミーユーザーを削除
     */
    private void ensureEvenCount() {
        long realCount = getRealParticipantCount();
        
        // 既存のダミーユーザーを削除
        removeExistingDummy();
        
        // 0名の場合はダミー追加なし
        if (realCount == 0) {
            return;
        }
        
        // 奇数の場合のみダミーユーザーを追加
        if (realCount % 2 == 1) {
            Participant dummy = createDummyParticipant();
            participants.add(dummy);
        }
    }

    /**
     * 既存のダミーユーザーを削除する
     */
    private void removeExistingDummy() {
        participants.removeIf(Participant::isDummy);
    }

    /**
     * ダミーユーザーを作成する
     * 
     * @return ダミーユーザー
     */
    private Participant createDummyParticipant() {
        int maxOrder = participants.stream()
            .mapToInt(Participant::getRegistrationOrder)
            .max()
            .orElse(0);
        
        return new Participant(
            UUID.randomUUID(),
            group.getGroupId(),
            null,
            "ダミーユーザー（不戦勝）",
            null,
            true,
            maxOrder + 1
        );
    }

    /**
     * 32名上限チェック
     * 
     * @param additionalCount 追加予定の参加者数
     * @throws DomainException 上限を超える場合
     */
    private void validateCapacity(int additionalCount) {
        long realCount = getRealParticipantCount();
        
        if (realCount + additionalCount > Group.MAX_PARTICIPANTS) {
            throw new DomainException(
                String.format("参加者数が上限（%d名）に達しています。現在%d名登録済みのため、追加可能なのは%d名までです。",
                    Group.MAX_PARTICIPANTS,
                    realCount,
                    Group.MAX_PARTICIPANTS - realCount)
            );
        }
    }

    /**
     * 段級位順→登録順でソートされた参加者リストを取得する
     * ダミーユーザーは末尾に配置される
     * 
     * @return ソート済み参加者リスト
     */
    public List<Participant> getSortedParticipants() {
        return participants.stream()
            .sorted((p1, p2) -> {
                // ダミーユーザーは末尾
                if (p1.isDummy() && !p2.isDummy()) return 1;
                if (!p1.isDummy() && p2.isDummy()) return -1;
                if (p1.isDummy() && p2.isDummy()) return 0;
                
                // 段級位順（降順）
                int rankCompare = p1.getRank().compareTo(p2.getRank());
                if (rankCompare != 0) {
                    return rankCompare;
                }
                
                // 同段級位の場合は登録順
                return Integer.compare(p1.getRegistrationOrder(), p2.getRegistrationOrder());
            })
            .collect(Collectors.toList());
    }

    /**
     * CSV出力用の参加者リストを取得する
     * ダミーユーザーを除外し、登録順でソートする
     * 
     * @return CSV出力用参加者リスト
     */
    public List<Participant> exportToCsv() {
        return participants.stream()
            .filter(p -> !p.isDummy())
            .sorted(Comparator.comparing(Participant::getRegistrationOrder))
            .collect(Collectors.toList());
    }

    /**
     * 指定数の参加者を追加可能かチェック
     * 
     * @param count 追加予定の参加者数
     * @return 追加可能な場合true
     */
    public boolean canAddParticipants(int count) {
        long realCount = getRealParticipantCount();
        return realCount + count <= Group.MAX_PARTICIPANTS;
    }

    /**
     * 参加者リストを取得する
     * 
     * @return 参加者リスト（不変）
     */
    public List<Participant> getParticipants() {
        return Collections.unmodifiableList(participants);
    }

    /**
     * 参加者数を取得する（ダミーユーザー含む）
     * 
     * @return 参加者数
     */
    public int getParticipantCount() {
        return participants.size();
    }

    /**
     * 実参加者数を取得する（ダミーユーザー除く）
     * 
     * @return 実参加者数
     */
    public long getRealParticipantCount() {
        return participants.stream()
            .filter(p -> !p.isDummy())
            .count();
    }

    /**
     * グループを取得する
     * 
     * @return グループ
     */
    public Group getGroup() {
        return group;
    }
}
