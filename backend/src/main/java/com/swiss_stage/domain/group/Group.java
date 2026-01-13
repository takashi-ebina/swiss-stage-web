package com.swiss_stage.domain.group;

import java.util.Objects;
import java.util.UUID;

/**
 * グループエンティティ
 * 
 * トーナメント内の参加者を分割する論理的なグループを表す。 1つのトーナメントには最大8グループまで作成でき、 各グループには最大32名の参加者が所属できる。
 * 
 * @param groupId グループID
 * @param tournamentId 所属トーナメントID
 * @param groupNumber グループ番号（1〜8）
 * @param displayName 表示名（例: "GROUP 1"）
 */
public record Group(
        UUID groupId,
        UUID tournamentId,
        int groupNumber,
        String displayName) {

    /**
     * 1グループあたりの参加者上限
     */
    public static final int MAX_PARTICIPANTS = 32;

    /**
     * 1トーナメントあたりのグループ上限
     */
    public static final int MAX_GROUPS = 8;

    /**
     * コンストラクタ（groupNumber指定版） displayNameは自動生成される
     * 
     * @param groupId グループID
     * @param tournamentId 所属トーナメントID
     * @param groupNumber グループ番号（1〜8）
     */
    public Group(UUID groupId, UUID tournamentId, int groupNumber) {
        this(groupId, tournamentId, groupNumber, "GROUP " + groupNumber);
    }

    /**
     * コンパクトコンストラクタ（バリデーション）
     * 
     * @throws NullPointerException groupId、tournamentId、displayNameがnullの場合
     * @throws IllegalArgumentException groupNumberが1〜8の範囲外の場合
     */
    public Group {
        Objects.requireNonNull(groupId, "groupIdは必須です");
        Objects.requireNonNull(tournamentId, "tournamentIdは必須です");
        Objects.requireNonNull(displayName, "displayNameは必須です");

        if (groupNumber < 1 || groupNumber > MAX_GROUPS) {
            throw new IllegalArgumentException(
                    "groupNumberは1〜" + MAX_GROUPS + "の範囲内である必要があります: " + groupNumber);
        }
    }
}
