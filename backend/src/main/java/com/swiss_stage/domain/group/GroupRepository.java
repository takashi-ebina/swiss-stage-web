package com.swiss_stage.domain.group;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Groupエンティティのリポジトリインターフェース ドメイン層が定義し、インフラ層が実装する
 */
public interface GroupRepository {

    /**
     * トーナメントIDとグループ番号でグループを検索
     * 
     * @param tournamentId トーナメントID
     * @param groupNumber グループ番号 (1-8)
     * @return グループ（存在しない場合は空）
     */
    Optional<Group> findByTournamentIdAndGroupNumber(UUID tournamentId, int groupNumber);

    /**
     * トーナメントIDで全グループを検索
     * 
     * @param tournamentId トーナメントID
     * @return グループリスト（groupNumber順）
     */
    List<Group> findByTournamentId(UUID tournamentId);

    /**
     * グループを保存（新規作成 or 更新）
     * 
     * @param group 保存対象のグループ
     * @return 保存されたグループ
     */
    Group save(Group group);

    /**
     * グループを削除
     * 
     * @param tournamentId トーナメントID
     * @param groupNumber グループ番号
     */
    void delete(UUID tournamentId, int groupNumber);
}
