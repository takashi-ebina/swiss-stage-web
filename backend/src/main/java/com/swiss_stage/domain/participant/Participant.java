package com.swiss_stage.domain.participant;

import java.util.Objects;
import java.util.UUID;

/**
 * 参加者エンティティ
 * 
 * グループ内の参加者を表す。通常の参加者とダミーユーザーの2種類が存在する。 ダミーユーザーは参加者が奇数の場合に自動的に追加され、不戦勝として扱われる。
 */
public class Participant {

    private final UUID participantId;
    private final UUID groupId;
    private final String affiliation; // 所属（任意）
    private final String name; // 氏名（必須）
    private final Rank rank; // 段級位（通常参加者は必須、ダミーはnull）
    private final boolean isDummy; // ダミーユーザーフラグ
    private final int registrationOrder; // 登録順（同段級位時のソートに使用）

    /**
     * コンストラクタ
     * 
     * @param participantId 参加者ID
     * @param groupId 所属グループID
     * @param affiliation 所属（null許容、100文字以内）
     * @param name 氏名（必須、50文字以内）
     * @param rank 段級位（通常参加者は必須、ダミーユーザーはnull）
     * @param isDummy ダミーユーザーフラグ
     * @param registrationOrder 登録順
     * @throws NullPointerException participantIdまたはgroupIdがnullの場合
     * @throws IllegalArgumentException バリデーションエラーの場合
     */
    public Participant(
            UUID participantId,
            UUID groupId,
            String affiliation,
            String name,
            Rank rank,
            boolean isDummy,
            int registrationOrder) {
        this.participantId = Objects.requireNonNull(participantId, "participantIdは必須です");
        this.groupId = Objects.requireNonNull(groupId, "groupIdは必須です");

        // nameバリデーション
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("nameは必須です");
        }
        if (name.length() > 50) {
            throw new IllegalArgumentException("nameは50文字以内で入力してください: " + name.length() + "文字");
        }
        this.name = name;

        // affiliationバリデーション
        if (affiliation != null && affiliation.length() > 100) {
            throw new IllegalArgumentException(
                    "affiliationは100文字以内で入力してください: " + affiliation.length() + "文字");
        }
        this.affiliation = affiliation;

        // rankバリデーション（通常参加者は必須）
        if (!isDummy && rank == null) {
            throw new IllegalArgumentException("通常参加者のrankは必須です");
        }
        this.rank = rank;

        this.isDummy = isDummy;
        this.registrationOrder = registrationOrder;
    }

    /**
     * 参加者IDを取得
     * 
     * @return 参加者ID
     */
    public UUID getParticipantId() {
        return participantId;
    }

    /**
     * 所属グループIDを取得
     * 
     * @return グループID
     */
    public UUID getGroupId() {
        return groupId;
    }

    /**
     * 所属を取得
     * 
     * @return 所属（null可）
     */
    public String getAffiliation() {
        return affiliation;
    }

    /**
     * 氏名を取得
     * 
     * @return 氏名
     */
    public String getName() {
        return name;
    }

    /**
     * 段級位を取得
     * 
     * @return 段級位（ダミーユーザーの場合はnull）
     */
    public Rank getRank() {
        return rank;
    }

    /**
     * ダミーユーザーかどうかを判定
     * 
     * @return ダミーユーザーの場合true
     */
    public boolean isDummy() {
        return isDummy;
    }

    /**
     * 登録順を取得
     * 
     * @return 登録順
     */
    public int getRegistrationOrder() {
        return registrationOrder;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        Participant that = (Participant) o;
        return Objects.equals(participantId, that.participantId) &&
                Objects.equals(groupId, that.groupId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(participantId, groupId);
    }

    @Override
    public String toString() {
        return "Participant{" +
                "participantId=" + participantId +
                ", groupId=" + groupId +
                ", affiliation='" + affiliation + '\'' +
                ", name='" + name + '\'' +
                ", rank=" + rank +
                ", isDummy=" + isDummy +
                ", registrationOrder=" + registrationOrder +
                '}';
    }
}
