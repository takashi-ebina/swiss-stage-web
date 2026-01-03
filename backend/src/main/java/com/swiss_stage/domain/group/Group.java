package com.swiss_stage.domain.group;

import java.util.Objects;
import java.util.UUID;

/**
 * グループエンティティ
 * 
 * トーナメント内の参加者を分割する論理的なグループを表す。 1つのトーナメントには最大8グループまで作成でき、 各グループには最大32名の参加者が所属できる。
 */
public class Group {

    /**
     * 1グループあたりの参加者上限
     */
    public static final int MAX_PARTICIPANTS = 32;

    /**
     * 1トーナメントあたりのグループ上限
     */
    public static final int MAX_GROUPS = 8;

    private final UUID groupId;
    private final UUID tournamentId;
    private final int groupNumber;
    private final String displayName;

    /**
     * コンストラクタ
     * 
     * @param groupId グループID
     * @param tournamentId 所属トーナメントID
     * @param groupNumber グループ番号（1〜8）
     * @throws NullPointerException groupIdまたはtournamentIdがnullの場合
     * @throws IllegalArgumentException groupNumberが1〜8の範囲外の場合
     */
    public Group(UUID groupId, UUID tournamentId, int groupNumber) {
        this.groupId = Objects.requireNonNull(groupId, "groupIdは必須です");
        this.tournamentId = Objects.requireNonNull(tournamentId, "tournamentIdは必須です");

        if (groupNumber < 1 || groupNumber > MAX_GROUPS) {
            throw new IllegalArgumentException(
                    "groupNumberは1〜" + MAX_GROUPS + "の範囲内である必要があります: " + groupNumber);
        }

        this.groupNumber = groupNumber;
        this.displayName = "GROUP " + groupNumber;
    }

    /**
     * グループIDを取得
     * 
     * @return グループID
     */
    public UUID getGroupId() {
        return groupId;
    }

    /**
     * 所属トーナメントIDを取得
     * 
     * @return トーナメントID
     */
    public UUID getTournamentId() {
        return tournamentId;
    }

    /**
     * グループ番号を取得
     * 
     * @return グループ番号（1〜8）
     */
    public int getGroupNumber() {
        return groupNumber;
    }

    /**
     * 表示名を取得
     * 
     * @return 表示名（例: "GROUP 1"）
     */
    public String getDisplayName() {
        return displayName;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        Group group = (Group) o;
        return groupNumber == group.groupNumber &&
                Objects.equals(groupId, group.groupId) &&
                Objects.equals(tournamentId, group.tournamentId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(groupId, tournamentId, groupNumber);
    }

    @Override
    public String toString() {
        return "Group{" +
                "groupId=" + groupId +
                ", tournamentId=" + tournamentId +
                ", groupNumber=" + groupNumber +
                ", displayName='" + displayName + '\'' +
                '}';
    }
}
