package com.swiss_stage.domain.participant;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * GroupParticipantListエンティティのリポジトリインターフェース ドメイン層が定義し、インフラ層が実装する
 */
public interface GroupParticipantListRepository {

    /**
     * グループIDで参加者リストを検索
     * 
     * @param groupId グループID
     * @return 参加者リスト（存在しない場合は空）
     */
    Optional<GroupParticipantList> findByGroupId(UUID groupId);

    /**
     * グループIDで段級位順にソートされた参加者を検索 GSI (groupId-rankLevel-index) を使用
     * 
     * @param groupId グループID
     * @return 段級位順の参加者リスト
     */
    List<Participant> findParticipantsSortedByRank(UUID groupId);

    /**
     * 参加者リストを保存（集約全体を永続化） 既存の参加者を削除して新規に追加
     * 
     * @param list 保存対象の参加者リスト
     * @return 保存された参加者リスト
     */
    GroupParticipantList save(GroupParticipantList list);

    /**
     * グループの全参加者を削除
     * 
     * @param groupId グループID
     */
    void deleteAllByGroupId(UUID groupId);
}
