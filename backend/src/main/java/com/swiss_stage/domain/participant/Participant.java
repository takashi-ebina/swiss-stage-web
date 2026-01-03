package com.swiss_stage.domain.participant;

import java.util.Objects;
import java.util.UUID;

/**
 * 参加者エンティティ
 * 
 * グループ内の参加者を表す。通常の参加者とダミーユーザーの2種類が存在する。 ダミーユーザーは参加者が奇数の場合に自動的に追加され、不戦勝として扱われる。
 * 
 * @param participantId 参加者ID
 * @param groupId 所属グループID
 * @param affiliation 所属（null許容、100文字以内）
 * @param name 氏名（必須、50文字以内）
 * @param rank 段級位（必須、ダミーユーザーの場合は20級）
 * @param isDummy ダミーユーザーフラグ
 * @param registrationOrder 登録順（同段級位時のソートに使用）
 */
public record Participant(
        UUID participantId,
        UUID groupId,
        String affiliation,
        String name,
        Rank rank,
        boolean isDummy,
        int registrationOrder) {

    /**
     * コンパクトコンストラクタ（バリデーション）
     * 
     * @throws NullPointerException participantId、groupId、name、rankがnullの場合
     * @throws IllegalArgumentException バリデーションエラーの場合
     */
    public Participant {
        Objects.requireNonNull(participantId, "participantIdは必須です");
        Objects.requireNonNull(groupId, "groupIdは必須です");
        Objects.requireNonNull(name, "nameは必須です");
        Objects.requireNonNull(rank, "rankは必須です");

        // nameバリデーション
        if (name.isBlank()) {
            throw new IllegalArgumentException("nameは空白のみにできません");
        }
        if (name.length() > 50) {
            throw new IllegalArgumentException("nameは50文字以内で入力してください: " + name.length() + "文字");
        }

        // affiliationバリデーション
        if (affiliation != null && affiliation.length() > 100) {
            throw new IllegalArgumentException(
                    "affiliationは100文字以内で入力してください: " + affiliation.length() + "文字");
        }
    }
}
